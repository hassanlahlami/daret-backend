package ma.daret.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Binds {@code daret.auth.rate-limit.*}. */
@ConfigurationProperties(prefix = "daret.auth.rate-limit")
public record AuthRateLimitProperties(int maxRequests, int windowSeconds) {
}
