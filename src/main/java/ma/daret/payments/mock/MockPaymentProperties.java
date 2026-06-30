package ma.daret.payments.mock;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Tunes the mock provider. {@code failureRate} (0.0–1.0) lets tests exercise the
 * retry/default paths; {@code settleImmediately} makes {@code collect}/{@code disburse}
 * return SETTLED at once (otherwise PENDING until polled).
 */
@ConfigurationProperties(prefix = "daret.payments.mock")
public record MockPaymentProperties(double failureRate, boolean settleImmediately) {

    public MockPaymentProperties {
        if (failureRate < 0 || failureRate > 1) {
            throw new IllegalArgumentException("failureRate must be between 0 and 1");
        }
    }
}
