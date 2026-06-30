package ma.daret.identity.auth;

import java.util.UUID;

/**
 * The authenticated principal placed in the security context by
 * {@link JwtAuthenticationFilter}. Inject into controllers with
 * {@code @AuthenticationPrincipal}.
 */
public record AuthPrincipal(UUID userId) {
}
