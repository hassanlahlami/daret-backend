package ma.daret.identity.kyc;

import java.util.UUID;

/**
 * The KYC/identity-verification partner boundary (spec FR-3 / G4). The platform
 * never handles raw identity documents — it only initiates a verification
 * session with the partner and later receives the decision by webhook
 * ({@code /api/v1/webhooks/kyc}, already wired).
 *
 * <p>MVP: {@link MockKycProvider}. Prod: an eKYC partner adapter
 * ({@link ma.daret.identity.kyc.http.HttpKycProvider}), config-selected.
 */
public interface KycProvider {

    /** Start a verification session for a user. */
    KycSession initiate(UUID userId);

    /**
     * @param partnerRef  opaque reference to correlate the webhook decision
     * @param redirectUrl where to send the user to complete verification
     * @param status      initial status (typically PENDING)
     */
    record KycSession(String partnerRef, String redirectUrl, String status) {
    }
}
