package ma.daret.payments;

import ma.daret.circles.Circle;
import ma.daret.circles.CircleRepository;
import ma.daret.circles.CircleStatus;
import ma.daret.circles.Contribution;
import ma.daret.circles.ContributionRepository;
import ma.daret.circles.Payout;
import ma.daret.circles.PayoutRepository;
import ma.daret.payments.outbox.OutboxEvent;
import ma.daret.payments.outbox.OutboxEventRepository;
import ma.daret.payments.port.PaymentDirection;
import ma.daret.payments.port.PaymentStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Issues DISBURSE intents for a cycle once all of that cycle's contributions have
 * settled (spec FR-14). A payout is issued at most once (its {@code paymentIntentId}
 * guards re-issue), so a member can never be paid twice.
 */
@Service
public class PayoutService {

    public static final String EVENT_TYPE = "DISBURSE_PAYOUT";

    private final CircleRepository circles;
    private final ContributionRepository contributions;
    private final PayoutRepository payouts;
    private final PaymentIntentRepository intents;
    private final OutboxEventRepository outbox;

    public PayoutService(CircleRepository circles,
                         ContributionRepository contributions,
                         PayoutRepository payouts,
                         PaymentIntentRepository intents,
                         OutboxEventRepository outbox) {
        this.circles = circles;
        this.contributions = contributions;
        this.payouts = payouts;
        this.intents = intents;
        this.outbox = outbox;
    }

    /** @return number of new DISBURSE intents enqueued across all active circles. */
    @Transactional
    public int issueDuePayouts() {
        int issued = 0;
        for (Circle circle : circles.findAll()) {
            if (circle.getStatus() != CircleStatus.ACTIVE) {
                continue;
            }
            for (Payout payout : payouts.findByCircleIdAndStatus(circle.getId(), Payout.Status.SCHEDULED)) {
                if (payout.getPaymentIntentId() != null) {
                    continue;
                }
                boolean cycleFullyPaid = contributions.countByCircleIdAndCycleIndexAndStatusNot(
                        circle.getId(), payout.getCycleIndex(), Contribution.Status.PAID) == 0;
                if (!cycleFullyPaid) {
                    continue;
                }

                String key = "disburse:" + payout.getId();
                if (intents.findByIdempotencyKey(key).isPresent()) {
                    continue;
                }
                PaymentIntent intent = new PaymentIntent();
                intent.setDirection(PaymentDirection.DISBURSE);
                intent.setAmount(payout.getAmount());
                intent.setStatus(PaymentStatus.PENDING);
                intent.setIdempotencyKey(key);
                intent.setRefType("PAYOUT");
                intent.setRefId(payout.getId());
                intents.save(intent);

                payout.setPaymentIntentId(intent.getId());
                payouts.save(payout);

                outbox.save(new OutboxEvent(EVENT_TYPE, intent.getId().toString()));
                issued++;
            }
        }
        return issued;
    }
}
