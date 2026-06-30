package ma.daret.circles;

import ma.daret.circles.CircleDtos.CircleDetailView;
import ma.daret.circles.CircleDtos.CircleView;
import ma.daret.circles.CircleDtos.CreateCircleRequest;
import ma.daret.circles.CircleDtos.MemberView;
import ma.daret.common.web.ApiException;
import ma.daret.economics.FeeScheduleRepository;
import ma.daret.identity.kyc.KycGate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class CircleService {

    private final CircleRepository circles;
    private final CircleMemberRepository members;
    private final CircleStateMachine stateMachine;
    private final KycGate kycGate;
    private final FeeScheduleRepository feeSchedules;

    public CircleService(CircleRepository circles,
                         CircleMemberRepository members,
                         CircleStateMachine stateMachine,
                         KycGate kycGate,
                         FeeScheduleRepository feeSchedules) {
        this.circles = circles;
        this.members = members;
        this.stateMachine = stateMachine;
        this.kycGate = kycGate;
        this.feeSchedules = feeSchedules;
    }

    /** Create a circle (FORMING) and enroll the creator as its first member. */
    @Transactional
    public CircleView create(UUID ownerId, CreateCircleRequest req) {
        kycGate.requireVerified(ownerId);

        UUID feeScheduleId = req.feeScheduleId() != null
                ? req.feeScheduleId() : FeeScheduleRepository.DEFAULT_ID;
        if (!feeSchedules.existsById(feeScheduleId)) {
            throw ApiException.badRequest("FEE_SCHEDULE_NOT_FOUND", "Fee schedule does not exist");
        }

        Circle circle = new Circle();
        circle.setName(req.name().trim());
        circle.setContributionAmount(req.contributionAmount());
        circle.setSeatCount(req.seatCount());
        circle.setCycleLengthDays(req.cycleLengthDays());
        circle.setShariaMode(req.shariaMode());
        circle.setFeeScheduleId(feeScheduleId);
        circle.setOwnerId(ownerId);
        circle.setStatus(CircleStatus.FORMING);
        circles.save(circle);

        SlotPreference ownerPref = req.ownerSlotPreference() != null
                ? req.ownerSlotPreference() : SlotPreference.FLEXIBLE;
        members.save(new CircleMember(circle.getId(), ownerId, ownerPref));

        return CircleView.of(circle, 1);
    }

    /** Join a FORMING circle expressing a slot preference (spec FR-8). */
    @Transactional
    public CircleView join(UUID circleId, UUID userId, SlotPreference preference) {
        kycGate.requireVerified(userId);

        Circle circle = circles.findByIdForUpdate(circleId)
                .orElseThrow(() -> ApiException.notFound("CIRCLE_NOT_FOUND", "Circle not found"));

        if (circle.getStatus() != CircleStatus.FORMING) {
            throw ApiException.conflict("CIRCLE_NOT_OPEN", "Circle is not open for joining");
        }
        if (members.existsByCircleIdAndUserId(circleId, userId)) {
            throw ApiException.conflict("ALREADY_MEMBER", "Already a member of this circle");
        }
        int filled = members.countByCircleId(circleId);
        if (filled >= circle.getSeatCount()) {
            throw ApiException.conflict("CIRCLE_FULL", "No seats remaining");
        }

        members.save(new CircleMember(circleId, userId, preference));
        return CircleView.of(circle, filled + 1);
    }

    /** Owner (or ADMIN, enforced at the controller) dissolves a FORMING circle. */
    @Transactional
    public CircleView cancel(UUID circleId, UUID actorId, boolean isAdmin) {
        Circle circle = circles.findById(circleId)
                .orElseThrow(() -> ApiException.notFound("CIRCLE_NOT_FOUND", "Circle not found"));
        if (!isAdmin && !circle.getOwnerId().equals(actorId)) {
            throw new ApiException(org.springframework.http.HttpStatus.FORBIDDEN,
                    "NOT_OWNER", "Only the owner or an admin may cancel this circle");
        }
        stateMachine.transition(circle, CircleStatus.DISSOLVED);
        return CircleView.of(circle, members.countByCircleId(circleId));
    }

    @Transactional(readOnly = true)
    public CircleDetailView detail(UUID circleId) {
        Circle circle = circles.findById(circleId)
                .orElseThrow(() -> ApiException.notFound("CIRCLE_NOT_FOUND", "Circle not found"));
        List<CircleMember> circleMembers = members.findByCircleId(circleId);
        List<MemberView> memberViews = circleMembers.stream().map(MemberView::from).toList();
        return new CircleDetailView(CircleView.of(circle, circleMembers.size()), memberViews);
    }

    /** Discover circles, filterable by status, contribution range, cadence and Sharia mode. */
    @Transactional(readOnly = true)
    public Page<CircleView> discover(CircleStatus status,
                                     BigDecimal minAmount,
                                     BigDecimal maxAmount,
                                     Integer cycleLengthDays,
                                     Boolean shariaMode,
                                     Pageable pageable) {
        Specification<Circle> spec = (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> p = new ArrayList<>();
            // Default discovery surface is open (FORMING) circles.
            p.add(cb.equal(root.get("status"), status != null ? status : CircleStatus.FORMING));
            if (minAmount != null) {
                p.add(cb.greaterThanOrEqualTo(root.get("contributionAmount"), minAmount));
            }
            if (maxAmount != null) {
                p.add(cb.lessThanOrEqualTo(root.get("contributionAmount"), maxAmount));
            }
            if (cycleLengthDays != null) {
                p.add(cb.equal(root.get("cycleLengthDays"), cycleLengthDays));
            }
            if (shariaMode != null) {
                p.add(cb.equal(root.get("shariaMode"), shariaMode));
            }
            return cb.and(p.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };

        Page<Circle> page = circles.findAll(spec, pageable);
        // filledSeats per row — fine at MVP discovery volumes.
        return page.map(c -> CircleView.of(c, members.countByCircleId(c.getId())));
    }
}
