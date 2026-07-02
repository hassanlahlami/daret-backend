package ma.daret.payments.outbox;

import ma.daret.common.web.ApiException;
import ma.daret.payments.CollectionService;
import ma.daret.payments.PayoutService;
import ma.daret.payments.PaymentIntent;
import ma.daret.payments.PaymentIntentRepository;
import ma.daret.payments.SettlementService;
import ma.daret.payments.port.PaymentDirection;
import ma.daret.payments.port.PaymentProvider;
import ma.daret.payments.port.PaymentProvider.CollectRequest;
import ma.daret.payments.port.PaymentProvider.DisburseRequest;
import ma.daret.payments.port.PaymentProvider.PaymentResult;
import ma.daret.payments.port.PaymentStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Handles one outbox event in its own transaction: sends the intent across the
 * {@link PaymentProvider} port, records the partner's reference/status, and — if
 * the partner already settled — applies the settlement. Idempotent per event.
 */
@Component
public class OutboxProcessor {

    private final PaymentIntentRepository intents;
    private final PaymentProvider paymentProvider;
    private final SettlementService settlement;

    public OutboxProcessor(PaymentIntentRepository intents,
                           PaymentProvider paymentProvider,
                           SettlementService settlement) {
        this.intents = intents;
        this.paymentProvider = paymentProvider;
        this.settlement = settlement;
    }

    @Transactional
    public void process(UUID eventId, OutboxEventRepository outbox) {
        OutboxEvent event = outbox.findById(eventId).orElseThrow();
        if (event.getStatus() == OutboxEvent.Status.PROCESSED) {
            return;
        }
        event.setAttempts(event.getAttempts() + 1);

        PaymentIntent intent = intents.findById(UUID.fromString(event.getPayload()))
                .orElseThrow(() -> ApiException.notFound("INTENT_NOT_FOUND", "Payment intent not found"));

        // Only send intents that haven't already settled (at-least-once safe).
        if (intent.getStatus() == PaymentStatus.PENDING) {
            PaymentResult result = dispatch(event.getType(), intent);
            intent.setExternalRef(result.externalRef());
            intent.setStatus(result.status());
            intents.save(intent);
        }

        if (intent.getStatus() == PaymentStatus.SETTLED) {
            if (intent.getDirection() == PaymentDirection.COLLECT) {
                settlement.applyCollectSettled(intent);
            } else {
                settlement.applyDisburseSettled(intent);
            }
        }

        event.setStatus(OutboxEvent.Status.PROCESSED);
        event.setProcessedAt(Instant.now());
        outbox.save(event);
    }

    private PaymentResult dispatch(String type, PaymentIntent intent) {
        String party = intent.getRefType() + ":" + intent.getRefId();
        String desc = type;
        return switch (type) {
            case CollectionService.EVENT_TYPE ->
                    paymentProvider.collect(new CollectRequest(intent.getIdempotencyKey(), intent.getAmount(), party, desc));
            case PayoutService.EVENT_TYPE ->
                    paymentProvider.disburse(new DisburseRequest(intent.getIdempotencyKey(), intent.getAmount(), party, desc));
            default -> throw new IllegalStateException("Unknown outbox event type: " + type);
        };
    }
}
