package ma.daret.identity.kyc.http;

import ma.daret.identity.kyc.KycProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.UUID;

/**
 * Real eKYC partner adapter skeleton (G4). Enabled with
 * {@code daret.kyc.provider=http}; default stays {@link ma.daret.identity.kyc.MockKycProvider}.
 *
 * <p><b>To go live</b> (see {@code docs/INTEGRATIONS.md}): set
 * {@code daret.kyc.http.base-url}/{@code api-key}, adapt {@code initiatePath} and
 * the request/response shape to the partner, and point the partner's webhook at
 * {@code /api/v1/webhooks/kyc} (already wired, HMAC-verified).
 */
@Component
@ConditionalOnProperty(value = "daret.kyc.provider", havingValue = "http")
public class HttpKycProvider implements KycProvider {

    private final HttpKycProperties props;
    private final RestClient client;

    public HttpKycProvider(HttpKycProperties props) {
        this.props = props;
        this.client = RestClient.builder()
                .baseUrl(props.baseUrl())
                .defaultHeader("Authorization", "Bearer " + props.apiKey())
                .build();
    }

    private record SessionResponse(String reference, String redirectUrl, String status) {
    }

    @Override
    public KycSession initiate(UUID userId) {
        SessionResponse resp = client.post()
                .uri(props.initiatePath())
                .body(Map.of("clientRef", userId.toString()))
                .retrieve()
                .body(SessionResponse.class);
        if (resp == null) {
            return new KycSession(null, null, "PENDING");
        }
        return new KycSession(resp.reference(), resp.redirectUrl(),
                resp.status() == null ? "PENDING" : resp.status());
    }
}
