package ma.daret.payments.port;

import java.math.BigDecimal;

/**
 * The regulatory firewall (spec §10.4). The platform never moves money itself —
 * every COLLECT/DISBURSE crosses this port to a licensed partner. Nothing in the
 * system may leak around this boundary.
 *
 * <p>MVP: {@code MockPaymentProvider}. Prod: an adapter for the licensed PSP
 * (Virement Instantané / MarocPay), swapped in without touching callers.
 */
public interface PaymentProvider {

    PaymentResult collect(CollectRequest request);

    PaymentResult disburse(DisburseRequest request);

    /** Reconcile a previously issued movement by its external reference. */
    PaymentStatus poll(String externalRef);

    /** Pull a contribution. {@code idempotencyKey} dedupes retries at the partner. */
    record CollectRequest(String idempotencyKey, BigDecimal amount, String partyRef, String description) {
    }

    /** Send the pot to a recipient. */
    record DisburseRequest(String idempotencyKey, BigDecimal amount, String partyRef, String description) {
    }

    /** The partner's response: its own reference plus the (initial) status. */
    record PaymentResult(String externalRef, PaymentStatus status) {
    }
}
