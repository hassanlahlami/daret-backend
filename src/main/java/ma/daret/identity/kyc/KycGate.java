package ma.daret.identity.kyc;

import ma.daret.common.web.ApiException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Enforces the regulatory KYC gate (spec §4.5, FR-4): no user may create or join
 * a circle until their KYC profile is VERIFIED. Other modules depend on this
 * component rather than reaching into KYC internals.
 */
@Component
public class KycGate {

    private final KycProfileRepository repository;

    public KycGate(KycProfileRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public boolean isVerified(UUID userId) {
        return repository.findByUserId(userId)
                .map(p -> p.getStatus() == KycStatus.VERIFIED)
                .orElse(false);
    }

    /** Throws 403 unless the user's KYC is VERIFIED. */
    public void requireVerified(UUID userId) {
        if (!isVerified(userId)) {
            throw new ApiException(org.springframework.http.HttpStatus.FORBIDDEN,
                    "KYC_REQUIRED", "KYC verification is required for this action");
        }
    }
}
