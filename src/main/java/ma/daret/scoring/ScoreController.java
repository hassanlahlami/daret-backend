package ma.daret.scoring;

import ma.daret.identity.auth.AuthPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** The caller's own credit score (spec §11: GET /me/score). */
@RestController
@RequestMapping("/api/v1/me/score")
@PreAuthorize("hasRole('MEMBER')")
public class ScoreController {

    private final ScoringService scoringService;

    public ScoreController(ScoringService scoringService) {
        this.scoringService = scoringService;
    }

    public record ScoreResponse(UUID userId, int score, CreditTier tier,
                                Map<String, BigDecimal> factors, Instant computedAt) {
    }

    @GetMapping
    public ScoreResponse myScore(@AuthenticationPrincipal AuthPrincipal principal) {
        CreditScore s = scoringService.recompute(principal.userId());
        return new ScoreResponse(s.getUserId(), s.getScore(), s.getTier(), s.getFactors(), s.getComputedAt());
    }

    public record ScorePoint(int score, String tier, Instant at) {
    }

    @GetMapping("/history")
    public java.util.List<ScorePoint> history(@AuthenticationPrincipal AuthPrincipal principal) {
        return scoringService.history(principal.userId(), 30).stream()
                .map(s -> new ScorePoint(s.getScore(), s.getTier().name(), s.getCreatedAt()))
                .toList();
    }
}
