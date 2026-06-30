package ma.daret.identity.kyc;

import ma.daret.common.web.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class KycService {

    private final KycProfileRepository repository;

    public KycService(KycProfileRepository repository) {
        this.repository = repository;
    }

    /**
     * Submit (or re-submit) KYC. Stores only the opaque partner reference and
     * moves the profile to PENDING for partner/compliance review.
     */
    @Transactional
    public KycProfile submit(UUID userId, String partnerRef) {
        KycProfile profile = repository.findByUserId(userId).orElseGet(() -> new KycProfile(userId));
        profile.setPartnerRef(partnerRef);
        profile.setStatus(KycStatus.PENDING);
        return repository.save(profile);
    }

    @Transactional(readOnly = true)
    public KycProfile getForUser(UUID userId) {
        return repository.findByUserId(userId)
                .orElseThrow(() -> ApiException.notFound("KYC_NOT_FOUND", "No KYC profile for this user"));
    }
}
