package ma.daret.identity.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import ma.daret.identity.user.Role;
import ma.daret.identity.user.User;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

/**
 * Issues and validates short-lived access JWTs (HS256). The subject is the user
 * id; roles ride in a {@code roles} claim. Refresh tokens are opaque and handled
 * separately by {@link RefreshTokenService}.
 */
@Service
public class JwtService {

    private final SecretKey key;
    private final long accessTtlMinutes;

    public JwtService(JwtProperties props) {
        this.key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(props.secret()));
        this.accessTtlMinutes = props.accessTtlMinutes();
    }

    public String issueAccessToken(User user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("roles", user.getRoles().stream().map(Role::name).toList())
                .issuedAt(java.util.Date.from(now))
                .expiration(java.util.Date.from(now.plusSeconds(accessTtlMinutes * 60)))
                .signWith(key)
                .compact();
    }

    /**
     * Parses and verifies an access token. Throws if invalid/expired.
     */
    public ParsedToken parse(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        @SuppressWarnings("unchecked")
        List<String> roles = claims.get("roles", List.class);
        return new ParsedToken(UUID.fromString(claims.getSubject()), roles == null ? List.of() : roles);
    }

    public record ParsedToken(UUID userId, List<String> roles) {
    }
}
