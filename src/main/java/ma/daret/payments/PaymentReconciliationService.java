package ma.daret.payments;

import ma.daret.common.web.ApiException;
import ma.daret.payments.port.PaymentProvider;
import ma.daret.payments.port.PaymentStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Settles PENDING payment intents from the PSP's async confirmation — either
 * pushed via webhook ({@link #reconcileByExternalRef}) or pulled by the scheduled
 * poller ({@link #reconcilePending}). All operations are idempotent: an intent is
 * settled at most once regardless of how many confirmations arrive.
 */
@Service
public class PaymentReconciliationService {

    private final PaymentIntentRepository intents;
    private final PaymentProvider provider;
    private final SettlementService settlement;

    public PaymentReconciliationService(PaymentIntentRepository intents,
                                        PaymentProvider provider,
                                        SettlementService settlement) {
        this.intents = intents;
        this.provider = provider;
        this.settlement = settlement;
    }

    /** Webhook path: apply a status the PSP reported for one external reference. */
    @Transactional
    public void reconcileByExternalRef(String externalRef, PaymentStatus status) {
        PaymentIntent intent = intents.findByExternalRef(externalRef)
                .orElseThrow(() -> ApiException.notFound("INTENT_NOT_FOUND", "No intent for external ref"));
        apply(intent, status);
    }

    /** Poll path: reconcile every still-pending intent against the provider. */
    @Transactional
    public int reconcilePending() {
        int settled = 0;
        for (PaymentIntent intent : intents.findByStatus(PaymentStatus.PENDING)) {
            if (intent.getExternalRef() == null) {
                continue; // not yet sent to the PSP
            }
            if (apply(intent, provider.poll(intent.getExternalRef()))) {
                settled++;
            }
        }
        return settled;
    }

    /** @return true if this call transitioned the intent out of PENDING. */
    private boolean apply(PaymentIntent intent, PaymentStatus status) {
        if (intent.getStatus() != PaymentStatus.PENDING) {
            return false; // already resolved — idempotent
        }
        if (status == PaymentStatus.SETTLED) {
            intent.setStatus(PaymentStatus.SETTLED);
            intents.save(intent);
            settlement.applyForIntent(intent);
            return true;
        }
        if (status == PaymentStatus.FAILED) {
            intent.setStatus(PaymentStatus.FAILED);
            intents.save(intent);
            return true;
        }
        return false; // still pending
    }
}
