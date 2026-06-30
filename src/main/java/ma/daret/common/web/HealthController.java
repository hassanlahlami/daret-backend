package ma.daret.common.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/**
 * Public liveness ping the frontend uses to prove end-to-end wiring.
 * Deeper checks (DB, etc.) live under {@code /actuator/health}.
 */
@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

    @GetMapping
    public Map<String, Object> health() {
        return Map.of(
                "status", "UP",
                "service", "daret-backend",
                "time", Instant.now().toString()
        );
    }
}
