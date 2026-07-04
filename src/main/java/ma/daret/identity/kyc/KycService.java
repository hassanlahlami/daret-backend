package ma.daret.identity.kyc;

import ma.daret.common.web.ApiException;
import ma.daret.compliance.Audited;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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
    @Audited("kyc.submit")
    @Transactional
    public KycProfile submit(UUID userId, String partnerRef, boolean consent) {
        if (!consent) {
            throw ApiException.badRequest("CONSENT_REQUIRED", "CNDP data-processing consent is required");
        }
        KycProfile profile = repository.findByUserId(userId).orElseGet(() -> new KycProfile(userId));
        profile.setPartnerRef(partnerRef);
        profile.setConsentedAt(java.time.Instant.now());
        profile.setStatus(KycStatus.PENDING);
        return repository.save(profile);
    }

    @Transactional(readOnly = true)
    public KycProfile getForUser(UUID userId) {
        return repository.findByUserId(userId)
                .orElseThrow(() -> ApiException.notFound("KYC_NOT_FOUND", "No KYC profile for this user"));
    }

    /** Compliance review queue: profiles awaiting a decision (FR-19). */
    @Transactional(readOnly = true)
    public List<KycProfile> pendingQueue() {
        return repository.findByStatus(KycStatus.PENDING);
    }

    /**
     * Compliance decision on a KYC profile (FR-3, FR-19). Only VERIFIED or REJECTED
     * are valid outcomes.
     */
    @Audited("kyc.decision")
    @Transactional
    public KycProfile decide(UUID userId, KycStatus decision) {
        if (decision != KycStatus.VERIFIED && decision != KycStatus.REJECTED) {
            throw ApiException.badRequest("INVALID_KYC_DECISION", "Decision must be VERIFIED or REJECTED");
        }
        KycProfile profile = repository.findByUserId(userId)
                .orElseThrow(() -> ApiException.notFound("KYC_NOT_FOUND", "No KYC profile for this user"));
        profile.setStatus(decision);
        return repository.save(profile);
    }
}
