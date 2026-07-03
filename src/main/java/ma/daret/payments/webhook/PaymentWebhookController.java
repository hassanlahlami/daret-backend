package ma.daret.payments.webhook;

import com.fasterxml.jackson.databind.ObjectMapper;
import ma.daret.common.web.ApiException;
import ma.daret.payments.PaymentReconciliationService;
import ma.daret.payments.port.PaymentStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Inbound PSP settlement webhook (spec §10.4/§10.5). The PSP confirms a movement
 * asynchronously; we verify the HMAC signature over the raw body, then reconcile
 * the matching intent. Idempotent — replays settle at most once.
 */
@RestController
@RequestMapping("/api/v1/webhooks/psp")
public class PaymentWebhookController {

    private final WebhookProperties props;
    private final PaymentReconciliationService reconciliation;
    private final ObjectMapper objectMapper;

    public PaymentWebhookController(WebhookProperties props,
                                    PaymentReconciliationService reconciliation,
                                    ObjectMapper objectMapper) {
        this.props = props;
        this.reconciliation = reconciliation;
        this.objectMapper = objectMapper;
    }

    public record WebhookPayload(String externalRef, PaymentStatus status) {
    }

    @PostMapping
    public Map<String, Object> handle(@RequestHeader(value = "X-PSP-Signature", required = false) String signature,
                                      @RequestBody String rawBody) {
        if (!WebhookSignature.verify(props.secret(), rawBody, signature)) {
            throw ApiException.unauthorized("BAD_SIGNATURE", "Invalid webhook signature");
        }
        WebhookPayload payload;
        try {
            payload = objectMapper.readValue(rawBody, WebhookPayload.class);
        } catch (Exception e) {
            throw ApiException.badRequest("BAD_PAYLOAD", "Malformed webhook payload");
        }
        if (payload.externalRef() == null || payload.status() == null) {
            throw ApiException.badRequest("BAD_PAYLOAD", "externalRef and status are required");
        }
        reconciliation.reconcileByExternalRef(payload.externalRef(), payload.status());
        return Map.of("ok", true);
    }
}
