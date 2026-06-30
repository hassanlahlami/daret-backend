package ma.daret.circles;

import ma.daret.common.web.ApiException;
import ma.daret.economics.FeeEngine;
import ma.daret.economics.FeeSchedule;
import ma.daret.economics.FeeScheduleRepository;
import ma.daret.economics.SlotEconomics;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Activates a FORMING circle (spec FR-12): assigns slot positions, prices the
 * slots, and generates the full contribution + payout schedule, then transitions
 * the circle to ACTIVE.
 *
 * <p>Phase 4 uses a simple preference-ordered assignment (EARLY → FLEXIBLE → LATE,
 * then join order). Phase 5 replaces this with the real matching engine
 * (score gate, preference optimisation) and risk/gap-fill — without changing the
 * schedule-generation or money flow below.
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

    public CircleActivationService(CircleRepository circles,
                                   CircleMemberRepository members,
                                   SlotRepository slots,
                                   ContributionRepository contributions,
                                   PayoutRepository payouts,
                                   CircleStateMachine stateMachine,
                                   FeeEngine feeEngine,
                                   FeeScheduleRepository feeSchedules) {
        this.circles = circles;
        this.members = members;
        this.slots = slots;
        this.contributions = contributions;
        this.payouts = payouts;
        this.stateMachine = stateMachine;
        this.feeEngine = feeEngine;
        this.feeSchedules = feeSchedules;
    }

    public record ActivationResult(UUID circleId, CircleStatus status, int seats,
                                   int contributionsCreated, int payoutsCreated) {
    }

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

        List<CircleMember> roster = members.findByCircleId(circleId);
        int n = circle.getSeatCount();
        if (roster.size() != n) {
            // Gap-fill of an under-filled circle arrives in Phase 5 (risk/exposure cap).
            throw ApiException.conflict("CIRCLE_NOT_FULL",
                    "Circle has " + roster.size() + "/" + n + " seats filled");
        }

        FeeSchedule schedule = resolveSchedule(circle);

        // Order members by preference, then by join time, and assign positions 1..N.
        roster.sort(Comparator
                .comparingInt((CircleMember m) -> preferenceRank(m.getSlotPreference()))
                .thenComparing(CircleMember::getCreatedAt));

        BigDecimal contribution = circle.getContributionAmount();
        BigDecimal pot = contribution.multiply(BigDecimal.valueOf(n));
        LocalDate start = LocalDate.now();
        int contributionsCreated = 0;

        for (int i = 0; i < n; i++) {
            int position = i + 1;
            CircleMember member = roster.get(i);
            member.setSlotPosition(position);

            SlotEconomics econ = feeEngine.priceSlot(schedule, n, position, null, circle.isShariaMode());

            Slot slot = new Slot();
            slot.setCircleId(circleId);
            slot.setPosition(position);
            slot.setPositionFee(econ.positionFee());
            slot.setPayoutBonus(econ.payoutBonus());
            slot.setUserId(member.getUserId());
            slots.save(slot);

            // One contribution per cycle (the recipient also contributes in their cycle).
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

            // The member receives the pot once, at their slot's cycle, net of their economics.
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
        return new ActivationResult(circleId, circle.getStatus(), n, contributionsCreated, n);
    }

    private FeeSchedule resolveSchedule(Circle circle) {
        UUID id = circle.getFeeScheduleId() != null
                ? circle.getFeeScheduleId() : FeeScheduleRepository.DEFAULT_ID;
        return feeSchedules.findById(id)
                .orElseThrow(() -> ApiException.notFound("FEE_SCHEDULE_NOT_FOUND", "Fee schedule not found"));
    }

    private static int preferenceRank(SlotPreference p) {
        return switch (p) {
            case EARLY -> 0;
            case FLEXIBLE -> 1;
            case LATE -> 2;
        };
    }
}
