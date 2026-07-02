package ma.daret.circles;

import ma.daret.common.web.ApiException;
import ma.daret.compliance.Audited;
import ma.daret.economics.FeeEngine;
import ma.daret.economics.FeeSchedule;
import ma.daret.economics.FeeScheduleRepository;
import ma.daret.economics.SlotEconomics;
import ma.daret.identity.user.SystemUsers;
import ma.daret.risk.MatchingService;
import ma.daret.risk.MatchingService.Assignment;
import ma.daret.risk.MatchingService.Candidate;
import ma.daret.risk.RiskService;
import ma.daret.scoring.CreditTier;
import ma.daret.scoring.ScoringService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Activates a FORMING circle (spec FR-10/11/12/16): gap-fills any empty seats within
 * the exposure cap, runs the matching engine (score gate + preferences) to assign
 * positions, prices the slots, and generates the full contribution/payout schedule.
 */
@Service
public class CircleActivationService {

    private final CircleRepository circles;
    private final CircleMemberRepository members;
    private final SlotRepository slots;
    private final ContributionRepository contributions;
    private final PayoutRepository payouts;
    private final CircleStateMachine stateMachine;
    private final FeeEngine feeEngine;
    private final FeeScheduleRepository feeSchedules;
    private final RiskService riskService;
    private final MatchingService matchingService;
    private final ScoringService scoringService;

    public CircleActivationService(CircleRepository circles,
                                   CircleMemberRepository members,
                                   SlotRepository slots,
                                   ContributionRepository contributions,
                                   PayoutRepository payouts,
                                   CircleStateMachine stateMachine,
                                   FeeEngine feeEngine,
                                   FeeScheduleRepository feeSchedules,
                                   RiskService riskService,
                                   MatchingService matchingService,
                                   ScoringService scoringService) {
        this.circles = circles;
        this.members = members;
        this.slots = slots;
        this.contributions = contributions;
        this.payouts = payouts;
        this.stateMachine = stateMachine;
        this.feeEngine = feeEngine;
        this.feeSchedules = feeSchedules;
        this.riskService = riskService;
        this.matchingService = matchingService;
        this.scoringService = scoringService;
    }

    public record ActivationResult(UUID circleId, CircleStatus status, int seats, int gapFilledSeats,
                                   int contributionsCreated, int payoutsCreated, List<String> rationale) {
    }

    @Audited("circle.activate")
    @Transactional
    public ActivationResult activate(UUID circleId, UUID actorId, boolean isAdmin) {
        Circle circle = circles.findByIdForUpdate(circleId)
                .orElseThrow(() -> ApiException.notFound("CIRCLE_NOT_FOUND", "Circle not found"));
        if (!isAdmin && !circle.getOwnerId().equals(actorId)) {
            throw new ApiException(org.springframework.http.HttpStatus.FORBIDDEN,
                    "NOT_OWNER", "Only the owner or an admin may activate this circle");
        }
        if (circle.getStatus() != CircleStatus.FORMING) {
            throw ApiException.conflict("CIRCLE_NOT_FORMING", "Only a FORMING circle can be activated");
        }

        int n = circle.getSeatCount();
        List<CircleMember> roster = new ArrayList<>(members.findByCircleId(circleId));

        // Gap-fill empty seats with the platform account, within the exposure cap
        // (rejects activation if it would exceed it).
        RiskService.GapFillPlan plan = riskService.planGapFill(n, roster.size());
        for (int i = 0; i < plan.emptySeats(); i++) {
            CircleMember gap = new CircleMember(circleId, SystemUsers.PLATFORM_USER_ID, SlotPreference.FLEXIBLE);
            roster.add(members.save(gap));
        }

        // Match members to positions honouring the score gate and preferences.
        List<Candidate> candidates = roster.stream()
                .map(m -> new Candidate(m.getId(), m.getUserId(), tierOf(m.getUserId()),
                        m.getSlotPreference(), m.getCreatedAt(), isPlatform(m.getUserId())))
                .toList();
        List<Assignment> assignments = matchingService.assign(n, candidates);

        Map<UUID, CircleMember> byId = roster.stream()
                .collect(Collectors.toMap(CircleMember::getId, m -> m));
        FeeSchedule schedule = resolveSchedule(circle);
        BigDecimal contribution = circle.getContributionAmount();
        BigDecimal pot = contribution.multiply(BigDecimal.valueOf(n));
        LocalDate start = LocalDate.now();
        int contributionsCreated = 0;

        for (Assignment a : assignments) {
            CircleMember member = byId.get(a.memberId());
            int position = a.position();
            member.setSlotPosition(position);

            SlotEconomics econ = feeEngine.priceSlot(schedule, n, position, null, circle.isShariaMode());

            Slot slot = new Slot();
            slot.setCircleId(circleId);
            slot.setPosition(position);
            slot.setPositionFee(econ.positionFee());
            slot.setPayoutBonus(econ.payoutBonus());
            slot.setPlatformFunded(a.platformFunded());
            slot.setUserId(member.getUserId());
            slots.save(slot);

            for (int cycle = 1; cycle <= n; cycle++) {
                Contribution c = new Contribution();
                c.setCircleMemberId(member.getId());
                c.setCircleId(circleId);
                c.setUserId(member.getUserId());
                c.setCycleIndex(cycle);
                c.setAmount(contribution);
                c.setDueDate(start.plusDays((long) (cycle - 1) * circle.getCycleLengthDays()));
                contributions.save(c);
                contributionsCreated++;
            }

            BigDecimal amount = pot.add(econ.payoutBonus())
                    .subtract(econ.positionFee())
                    .subtract(econ.serviceFee());
            BigDecimal platformCut = econ.serviceFee()
                    .add(econ.positionFee())
                    .subtract(econ.payoutBonus());

            Payout payout = new Payout();
            payout.setCircleId(circleId);
            payout.setRecipientMemberId(member.getId());
            payout.setRecipientUserId(member.getUserId());
            payout.setCycleIndex(position);
            payout.setAmount(amount);
            payout.setPlatformCut(platformCut);
            payouts.save(payout);

            members.save(member);
        }

        stateMachine.transition(circle, CircleStatus.ACTIVE);
        List<String> rationale = assignments.stream().map(Assignment::rationale).toList();
        return new ActivationResult(circleId, circle.getStatus(), n, plan.emptySeats(),
                contributionsCreated, n, rationale);
    }

    private CreditTier tierOf(UUID userId) {
        return isPlatform(userId) ? CreditTier.A : scoringService.currentTier(userId);
    }

    private static boolean isPlatform(UUID userId) {
        return SystemUsers.PLATFORM_USER_ID.equals(userId);
    }

    private FeeSchedule resolveSchedule(Circle circle) {
        UUID id = circle.getFeeScheduleId() != null
                ? circle.getFeeScheduleId() : FeeScheduleRepository.DEFAULT_ID;
        return feeSchedules.findById(id)
                .orElseThrow(() -> ApiException.notFound("FEE_SCHEDULE_NOT_FOUND", "Fee schedule not found"));
    }
}
