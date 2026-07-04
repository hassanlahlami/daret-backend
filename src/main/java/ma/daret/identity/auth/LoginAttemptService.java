package ma.daret.identity.auth;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks failed logins per identifier and locks the account for a window after too
 * many failures — mitigates credential-stuffing/brute force (spec §15). In-memory
 * and per-instance; back with a shared store (Redis) when scaling horizontally.
 */
@Service
public class LoginAttemptService {

    private final LoginLockoutProperties props;
    private final ConcurrentHashMap<String, Attempt> attempts = new ConcurrentHashMap<>();

    public LoginAttemptService(LoginLockoutProperties props) {
        this.props = props;
    }

    private record Attempt(int count, Instant lockedUntil) {
    }

    public boolean isLocked(String key) {
        Attempt a = attempts.get(key);
        return a != null && a.lockedUntil() != null && a.lockedUntil().isAfter(Instant.now());
    }

    public void recordFailure(String key) {
        attempts.compute(key, (k, existing) -> {
            int count = (existing == null ? 0 : existing.count()) + 1;
            Instant lockedUntil = count >= props.maxAttempts()
                    ? Instant.now().plus(Duration.ofMinutes(props.lockMinutes()))
                    : null;
            return new Attempt(count, lockedUntil);
        });
    }

    public void reset(String key) {
        attempts.remove(key);
    }
}
