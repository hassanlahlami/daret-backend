package ma.daret.payments;

import ma.daret.circles.Contribution;
import ma.daret.circles.ContributionRepository;
import ma.daret.payments.outbox.OutboxEvent;
import ma.daret.payments.outbox.OutboxEventRepository;
import ma.daret.payments.port.PaymentDirection;
import ma.daret.payments.port.PaymentStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Issues COLLECT payment intents for contributions that are due (spec FR-13).
 * Idempotent: a contribution gets at most one intent (idempotency key + the
 * contribution's own {@code paymentIntentId} guard). The intent and its outbox
 * event are written in the same transaction as the contribution update.
 */
@Service
public class CollectionService {

    public static final String EVENT_TYPE = "COLLECT_CONTRIBUTION";

    private final ContributionRepository contributions;
    private final PaymentIntentRepository intents;
    private final OutboxEventRepository outbox;

    public CollectionService(ContributionRepository contributions,
                             PaymentIntentRepository intents,
                             OutboxEventRepository outbox) {
        this.contributions = contributions;
        this.intents = intents;
        this.outbox = outbox;
    }

    /** @return number of new COLLECT intents enqueued. */
    @Transactional
    public int issueDueCollections(LocalDate asOf) {
        List<Contribution> due = contributions
                .findByStatusAndDueDateLessThanEqual(Contribution.Status.SCHEDULED, asOf);
        int issued = 0;
        for (Contribution c : due) {
            if (c.getPaymentIntentId() != null) {
                continue;
            }
            String key = "collect:" + c.getId();
            if (intents.findByIdempotencyKey(key).isPresent()) {
                continue;
            }
            PaymentIntent intent = new PaymentIntent();
            intent.setDirection(PaymentDirection.COLLECT);
            intent.setAmount(c.getAmount());
            intent.setStatus(PaymentStatus.PENDING);
            intent.setIdempotencyKey(key);
            intent.setRefType("CONTRIBUTION");
            intent.setRefId(c.getId());
            intents.save(intent);

            c.setPaymentIntentId(intent.getId());
            contributions.save(c);

            outbox.save(new OutboxEvent(EVENT_TYPE, intent.getId().toString()));
            issued++;
        }
        return issued;
    }
}
