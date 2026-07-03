package ma.daret.payments.mock;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Tunes the mock provider.
 * <ul>
 *   <li>{@code failureRate} (0.0–1.0) exercises the retry/default paths;</li>
 *   <li>{@code settleImmediately} makes collect/disburse return SETTLED at once;</li>
 *   <li>{@code autoSettleOnPoll} makes a PENDING intent settle when polled — simulating
 *       async PSP settlement so the reconciler path can be exercised without a webhook.</li>
 * </ul>
 */
@ConfigurationProperties(prefix = "daret.payments.mock")
public record MockPaymentProperties(double failureRate, boolean settleImmediately, boolean autoSettleOnPoll) {

    public MockPaymentProperties {
        if (failureRate < 0 || failureRate > 1) {
            throw new IllegalArgumentException("failureRate must be between 0 and 1");
        }
    }
}
