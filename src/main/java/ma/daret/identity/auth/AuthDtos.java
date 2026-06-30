package ma.daret.identity.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Auth request/response payloads. Registration accepts email and/or phone; at
 * least one must be present (validated in the service).
 */
public final class AuthDtos {

    private AuthDtos() {
    }

    public record RegisterRequest(
            @Email(message = "must be a valid email") String email,
            String phone,
            @NotBlank @Size(min = 8, max = 100, message = "password must be 8-100 chars") String password
    ) {
    }

    public record LoginRequest(
            String email,
            String phone,
            @NotBlank String password
    ) {
    }

    public record RefreshRequest(
            @NotBlank String refreshToken
    ) {
    }

    public record TokenResponse(
            String accessToken,
            String refreshToken,
            String tokenType
    ) {
        public static TokenResponse bearer(String access, String refresh) {
            return new TokenResponse(access, refresh, "Bearer");
        }
    }
}
