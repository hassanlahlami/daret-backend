package ma.daret.payments.webhook;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Shared secret used to verify the HMAC signature on inbound PSP webhooks. */
@ConfigurationProperties(prefix = "daret.payments.webhook")
public record WebhookProperties(String secret) {
}
