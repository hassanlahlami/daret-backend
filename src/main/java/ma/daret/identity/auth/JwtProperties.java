package ma.daret.identity.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds {@code daret.jwt.*}. The secret is a Base64-encoded HMAC key and must be
 * overridden outside local development.
 */
@ConfigurationProperties(prefix = "daret.jwt")
public record JwtProperties(
        String secret,
        long accessTtlMinutes,
        long refreshTtlDays
) {
}
