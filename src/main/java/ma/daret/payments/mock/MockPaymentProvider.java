package ma.daret.payments.mock;

import ma.daret.payments.port.PaymentProvider;
import ma.daret.payments.port.PaymentStatus;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * In-memory stand-in for the licensed PSP (spec §10.4). Simulates settlement with
 * a configurable failure rate so the default / gap-fill paths can be exercised.
 * Idempotent on {@code idempotencyKey}: replaying a request returns the original
 * external reference and status rather than charging twice.
 */
@Component
public class MockPaymentProvider implements PaymentProvider {

    private final MockPaymentProperties props;
    private final ConcurrentHashMap<String, PaymentStatus> byExternalRef = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, PaymentResult> byIdempotencyKey = new ConcurrentHashMap<>();

    public MockPaymentProvider(MockPaymentProperties props) {
        this.props = props;
    }

    @Override
    public PaymentResult collect(CollectRequest request) {
        return process(request.idempotencyKey());
    }

    @Override
    public PaymentResult disburse(DisburseRequest request) {
        return process(request.idempotencyKey());
    }

    @Override
    public PaymentStatus poll(String externalRef) {
        // Simulate async PSP settlement: a pending movement settles once polled.
        if (props.autoSettleOnPoll()) {
            byExternalRef.computeIfPresent(externalRef,
                    (k, v) -> v == PaymentStatus.PENDING ? PaymentStatus.SETTLED : v);
        }
        return byExternalRef.getOrDefault(externalRef, PaymentStatus.PENDING);
    }

    private PaymentResult process(String idempotencyKey) {
        PaymentResult existing = byIdempotencyKey.get(idempotencyKey);
        if (existing != null) {
            return existing;
        }
        boolean failed = ThreadLocalRandom.current().nextDouble() < props.failureRate();
        PaymentStatus status = failed
                ? PaymentStatus.FAILED
                : (props.settleImmediately() ? PaymentStatus.SETTLED : PaymentStatus.PENDING);

        String externalRef = "mock_" + java.util.UUID.randomUUID();
        byExternalRef.put(externalRef, status);
        PaymentResult result = new PaymentResult(externalRef, status);
        byIdempotencyKey.put(idempotencyKey, result);
        return result;
    }

    /** Test/ops hook: force a pending external ref to settle. */
    public void markSettled(String externalRef) {
        byExternalRef.computeIfPresent(externalRef, (k, v) -> PaymentStatus.SETTLED);
    }
}
