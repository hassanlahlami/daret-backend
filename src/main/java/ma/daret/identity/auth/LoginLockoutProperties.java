package ma.daret.identity.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Account lockout after repeated failed logins. Binds {@code daret.auth.lockout.*}. */
@ConfigurationProperties(prefix = "daret.auth.lockout")
public record LoginLockoutProperties(int maxAttempts, int lockMinutes) {
}
