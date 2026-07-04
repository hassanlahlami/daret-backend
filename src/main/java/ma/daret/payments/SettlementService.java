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
import ma.daret.compliance.Audited;
import ma.daret.ledger.LedgerAccounts;
import ma.daret.ledger.LedgerService;
import ma.daret.ledger.LedgerService.Leg;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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
    private final ma.daret.compliance.AmlService amlService;
    private final ma.daret.risk.RiskProperties riskProps;

    public SettlementService(ContributionRepository contributions,
                             PayoutRepository payouts,
                             CircleRepository circles,
                             CircleStateMachine stateMachine,
                             LedgerService ledger,
                             ma.daret.compliance.AmlService amlService,
                             ma.daret.risk.RiskProperties riskProps) {
        this.contributions = contributions;
        this.payouts = payouts;
        this.circles = circles;
        this.stateMachine = stateMachine;
        this.ledger = ledger;
        this.amlService = amlService;
        this.riskProps = riskProps;
    }

    /** Dispatch settlement by the intent's direction. Idempotent. */
    @Transactional
    public void applyForIntent(PaymentIntent intent) {
        if (intent.getDirection() == ma.daret.payments.port.PaymentDirection.COLLECT) {
            applyCollectSettled(intent);
        } else {
            applyDisburseSettled(intent);
        }
    }

    @Audited("contribution.settled")
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

    @Audited("payout.settled")
    @Transactional
    public void applyDisburseSettled(PaymentIntent intent) {
        Payout p = payouts.findById(intent.getRefId())
                .orElseThrow(() -> ApiException.notFound("PAYOUT_NOT_FOUND", "Payout not found"));
        if (p.getStatus() == Payout.Status.PAID) {
            return; // already settled
        }
        p.setStatus(Payout.Status.PAID);
        payouts.save(p);

        // The pot (= recipient amount + platform cut) is debited and split between the
        // recipient and the platform. For late slots the platform cut is negative — the
        // platform tops up the recipient's bonus from fees earned on early slots — so that
        // leg becomes a debit on platform revenue. Every posting still nets to zero.
        BigDecimal cut = p.getPlatformCut();
        Leg platformLeg = cut.signum() >= 0
                ? Leg.credit(LedgerAccounts.PLATFORM_REVENUE, cut)
                : Leg.debit(LedgerAccounts.PLATFORM_REVENUE, cut.negate());
        ledger.post("PAYOUT", p.getId(), p.getCycleIndex(), List.of(
                Leg.debit(LedgerAccounts.circlePot(p.getCircleId()), p.getAmount().add(cut)),
                Leg.credit(LedgerAccounts.member(p.getRecipientUserId()), p.getAmount()),
                platformLeg
        ));

        // AML rule: flag unusually large disbursements for review.
        if (p.getAmount().compareTo(riskProps.amlLargeAmount()) > 0) {
            amlService.raise(p.getRecipientUserId(), p.getCircleId(),
                    ma.daret.compliance.AmlFlag.Type.LARGE_MOVEMENT, ma.daret.compliance.AmlFlag.Severity.MEDIUM,
                    "Large payout of " + p.getAmount() + " MAD");
        }

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
