package ma.daret.identity.kyc;

import com.fasterxml.jackson.databind.ObjectMapper;
import ma.daret.common.web.ApiException;
import ma.daret.payments.webhook.WebhookProperties;
import ma.daret.payments.webhook.WebhookSignature;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

/**
 * Inbound KYC-partner webhook: the partner confirms/rejects a verification
 * asynchronously (spec FR-3). HMAC-verified; drives the profile to VERIFIED/REJECTED
 * — the automated counterpart to the compliance officer's manual decision.
 */
@RestController
@RequestMapping("/api/v1/webhooks/kyc")
public class KycWebhookController {

    private final WebhookProperties props;
    private final KycService kycService;
    private final ObjectMapper objectMapper;

    public KycWebhookController(WebhookProperties props, KycService kycService, ObjectMapper objectMapper) {
        this.props = props;
        this.kycService = kycService;
        this.objectMapper = objectMapper;
    }

    public record KycWebhookPayload(UUID userId, KycStatus decision) {
    }

    @PostMapping
    public Map<String, Object> handle(@RequestHeader(value = "X-PSP-Signature", required = false) String signature,
                                      @RequestBody String rawBody) {
        if (!WebhookSignature.verify(props.secret(), rawBody, signature)) {
            throw ApiException.unauthorized("BAD_SIGNATURE", "Invalid webhook signature");
        }
        KycWebhookPayload payload;
        try {
            payload = objectMapper.readValue(rawBody, KycWebhookPayload.class);
        } catch (Exception e) {
            throw ApiException.badRequest("BAD_PAYLOAD", "Malformed webhook payload");
        }
        if (payload.userId() == null || payload.decision() == null) {
            throw ApiException.badRequest("BAD_PAYLOAD", "userId and decision are required");
        }
        kycService.decide(payload.userId(), payload.decision());
        return Map.of("ok", true);
    }
}
