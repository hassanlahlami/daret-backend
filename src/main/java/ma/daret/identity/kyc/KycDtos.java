package ma.daret.identity.kyc;

import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.util.UUID;

/**
 * Request/response payloads for the KYC endpoints.
 *
 * <p>MVP submission carries only an opaque {@code partnerRef} produced by the
 * KYC partner's SDK on the client — the platform never receives raw documents.
 */
public final class KycDtos {

    private KycDtos() {
    }

    public record SubmitKycRequest(
            @NotBlank(message = "partnerRef is required") String partnerRef,
            /** CNDP data-processing consent — required to proceed. */
            boolean consent
    ) {
    }

    public record KycStatusResponse(
            UUID id,
            UUID userId,
            KycStatus status,
            Instant updatedAt
    ) {
        public static KycStatusResponse from(KycProfile p) {
            return new KycStatusResponse(p.getId(), p.getUserId(), p.getStatus(), p.getUpdatedAt());
        }
    }
}
