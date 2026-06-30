package ma.daret.identity.auth;

import ma.daret.common.web.ApiException;
import ma.daret.identity.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Issues opaque refresh tokens and rotates them on use. The DB stores only the
 * SHA-256 hash; the raw token is returned to the client exactly once.
 */
@Service
public class RefreshTokenService {

    private static final SecureRandom RNG = new SecureRandom();

    private final RefreshTokenRepository repository;
    private final Duration ttl;

    public RefreshTokenService(RefreshTokenRepository repository, JwtProperties props) {
        this.repository = repository;
        this.ttl = Duration.ofDays(props.refreshTtlDays());
    }

    /** Mint a new refresh token for the user and return the raw value. */
    @Transactional
    public String issue(User user) {
        String raw = randomToken();
        RefreshToken entity = new RefreshToken();
        entity.setUserId(user.getId());
        entity.setTokenHash(hash(raw));
        entity.setExpiresAt(Instant.now().plus(ttl));
        repository.save(entity);
        return raw;
    }

    /**
     * Validate the presented refresh token, revoke it, and mint a replacement.
     * Returns the owning user id and the new raw token.
     */
    @Transactional
    public Rotation rotate(String rawToken) {
        RefreshToken current = repository.findByTokenHash(hash(rawToken))
                .orElseThrow(() -> ApiException.unauthorized("INVALID_REFRESH", "Refresh token not recognized"));
        if (!current.isActive(Instant.now())) {
            throw ApiException.unauthorized("INVALID_REFRESH", "Refresh token expired or revoked");
        }
        current.setRevokedAt(Instant.now());

        String raw = randomToken();
        RefreshToken next = new RefreshToken();
        next.setUserId(current.getUserId());
        next.setTokenHash(hash(raw));
        next.setExpiresAt(Instant.now().plus(ttl));
        repository.save(next);

        return new Rotation(current.getUserId(), raw);
    }

    private static String randomToken() {
        byte[] bytes = new byte[32];
        RNG.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String hash(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    public record Rotation(java.util.UUID userId, String rawToken) {
    }
}
