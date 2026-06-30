package ma.daret.identity.kyc;

/**
 * KYC lifecycle: {@code PENDING → VERIFIED | REJECTED}.
 * Only a VERIFIED profile may create or join a circle (enforced from Phase 2).
 */
public enum KycStatus {
    PENDING,
    VERIFIED,
    REJECTED
}
