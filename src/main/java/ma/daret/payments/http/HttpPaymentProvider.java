package ma.daret.payments.http;

import ma.daret.payments.port.PaymentProvider;
import ma.daret.payments.port.PaymentStatus;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;

/**
 * Real PSP adapter skeleton (spec §10.4 / G3). Talks to a licensed payment
 * partner over HTTPS. Enabled with {@code daret.payments.provider=http}; the
 * default remains {@link ma.daret.payments.mock.MockPaymentProvider} so nothing
 * changes until a provider is contracted.
 *
 * <p><b>To go live</b> (see {@code docs/INTEGRATIONS.md}):
 * <ol>
 *   <li>Set {@code daret.payments.http.base-url} / {@code api-key} (env).</li>
 *   <li>Adapt {@code collectPath}/{@code disbursePath}/{@code statusPath} and the
 *       request/response DTOs below to the provider's contract (CMI, PayZone…).</li>
 *   <li>Map the provider's status strings in {@link #mapStatus}.</li>
 *   <li>Point the PSP's webhook at {@code /api/v1/webhooks/psp} (already wired,
 *       HMAC-verified) so settlements reconcile asynchronously.</li>
 * </ol>
 * The regulatory boundary is unchanged: money still only moves through this port.
 */
@Component
@ConditionalOnProperty(value = "daret.payments.provider", havingValue = "http")
public class HttpPaymentProvider implements PaymentProvider {

    private final HttpPaymentProperties props;
    private final RestClient client;

    public HttpPaymentProvider(HttpPaymentProperties props) {
        this.props = props;
        this.client = RestClient.builder()
                .baseUrl(props.baseUrl())
                .defaultHeader("Authorization", "Bearer " + props.apiKey())
                .build();
    }

    /** Generic charge/transfer request — rename fields to match the PSP. */
    private record MovementRequest(String idempotencyKey, BigDecimal amount, String party, String description) {
    }

    /** Generic response — rename fields to match the PSP. */
    private record MovementResponse(String reference, String status) {
    }

    @Override
    public PaymentResult collect(CollectRequest request) {
        MovementResponse resp = client.post()
                .uri(props.collectPath())
                .header("Idempotency-Key", request.idempotencyKey())
                .body(new MovementRequest(request.idempotencyKey(), request.amount(),
                        request.partyRef(), request.description()))
                .retrieve()
                .body(MovementResponse.class);
        return toResult(resp);
    }

    @Override
    public PaymentResult disburse(DisburseRequest request) {
        MovementResponse resp = client.post()
                .uri(props.disbursePath())
                .header("Idempotency-Key", request.idempotencyKey())
                .body(new MovementRequest(request.idempotencyKey(), request.amount(),
                        request.partyRef(), request.description()))
                .retrieve()
                .body(MovementResponse.class);
        return toResult(resp);
    }

    @Override
    public PaymentStatus poll(String externalRef) {
        MovementResponse resp = client.get()
                .uri(props.statusPath(), externalRef)
                .retrieve()
                .body(MovementResponse.class);
        return resp == null ? PaymentStatus.PENDING : mapStatus(resp.status());
    }

    private PaymentResult toResult(MovementResponse resp) {
        if (resp == null) {
            return new PaymentResult(null, PaymentStatus.PENDING);
        }
        return new PaymentResult(resp.reference(), mapStatus(resp.status()));
    }

    /** Map the provider's status vocabulary onto our three canonical states. */
    private static PaymentStatus mapStatus(String status) {
        if (status == null) {
            return PaymentStatus.PENDING;
        }
        return switch (status.trim().toUpperCase()) {
            case "SETTLED", "SUCCEEDED", "SUCCESS", "PAID", "COMPLETED" -> PaymentStatus.SETTLED;
            case "FAILED", "DECLINED", "CANCELLED", "REJECTED" -> PaymentStatus.FAILED;
            default -> PaymentStatus.PENDING;
        };
    }
}
