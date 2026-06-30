package ma.daret.payments;

import ma.daret.circles.Circle;
import ma.daret.circles.CircleRepository;
import ma.daret.circles.CircleStateMachine;
import ma.daret.circles.CircleStatus;
import ma.daret.circles.Contribution;
import ma.daret.circles.ContributionRepository;
import ma.daret.circles.Payout;
import ma.daret.circles.PayoutRepository;
import ma.daret.common.web.ApiException;
import ma.daret.ledger.LedgerAccounts;
import ma.daret.ledger.LedgerService;
import ma.daret.ledger.LedgerService.Leg;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Applies the financial effects of a settled payment intent: flips the
 * contribution/payout status and writes the matching balanced ledger posting,
 * both in one transaction. Idempotent — re-applying a settled intent is a no-op.
 */
@Service
public class SettlementService {

    private final ContributionRepository contributions;
    private final PayoutRepository payouts;
    private final CircleRepository circles;
    private final CircleStateMachine stateMachine;
    private final LedgerService ledger;

    public SettlementService(ContributionRepository contributions,
                             PayoutRepository payouts,
                             CircleRepository circles,
                             CircleStateMachine stateMachine,
                             LedgerService ledger) {
        this.contributions = contributions;
        this.payouts = payouts;
        this.circles = circles;
        this.stateMachine = stateMachine;
        this.ledger = ledger;
    }

    @Transactional
    public void applyCollectSettled(PaymentIntent intent) {
        Contribution c = contributions.findById(intent.getRefId())
                .orElseThrow(() -> ApiException.notFound("CONTRIBUTION_NOT_FOUND", "Contribution not found"));
        if (c.getStatus() == Contribution.Status.PAID) {
            return; // already settled
        }
        c.setStatus(Contribution.Status.PAID);
        contributions.save(c);

        // Member pays in (debit member), the circle pot receives it (credit pot).
        ledger.post("CONTRIBUTION", c.getId(), c.getCycleIndex(), List.of(
                Leg.debit(LedgerAccounts.member(c.getUserId()), c.getAmount()),
                Leg.credit(LedgerAccounts.circlePot(c.getCircleId()), c.getAmount())
        ));
    }

    @Transactional
    public void applyDisburseSettled(PaymentIntent intent) {
        Payout p = payouts.findById(intent.getRefId())
                .orElseThrow(() -> ApiException.notFound("PAYOUT_NOT_FOUND", "Payout not found"));
        if (p.getStatus() == Payout.Status.PAID) {
            return; // already settled
        }
        p.setStatus(Payout.Status.PAID);
        payouts.save(p);

        // The pot pays the recipient and the platform; debits = credits = the pot.
        ledger.post("PAYOUT", p.getId(), p.getCycleIndex(), List.of(
                Leg.debit(LedgerAccounts.circlePot(p.getCircleId()), p.getAmount().add(p.getPlatformCut())),
                Leg.credit(LedgerAccounts.member(p.getRecipientUserId()), p.getAmount()),
                Leg.credit(LedgerAccounts.PLATFORM_REVENUE, p.getPlatformCut())
        ));

        completeCircleIfDone(p.getCircleId());
    }

    private void completeCircleIfDone(UUID circleId) {
        boolean allPaid = payouts.findByCircleId(circleId).stream()
                .allMatch(po -> po.getStatus() == Payout.Status.PAID);
        if (!allPaid) {
            return;
        }
        Circle circle = circles.findById(circleId).orElseThrow();
        if (circle.getStatus() == CircleStatus.ACTIVE) {
            stateMachine.transition(circle, CircleStatus.COMPLETED);
            circles.save(circle);
        }
    }
}
