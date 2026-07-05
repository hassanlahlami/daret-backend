package ma.daret.identity.kyc;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Stand-in KYC partner (default). Returns a synthetic session so the flow is
 * exercisable end-to-end without a real provider. Active by default
 * ({@code daret.kyc.provider=mock}); set to {@code http} for a real partner.
 */
@Component
@ConditionalOnProperty(value = "daret.kyc.provider", havingValue = "mock", matchIfMissing = true)
public class MockKycProvider implements KycProvider {

    @Override
    public KycSession initiate(UUID userId) {
        String ref = "mock-kyc-" + UUID.randomUUID();
        return new KycSession(ref, "https://kyc.mock.local/verify/" + ref, "PENDING");
    }
}
