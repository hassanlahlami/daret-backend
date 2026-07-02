package ma.daret.scoring;

import ma.daret.circles.Contribution;
import ma.daret.circles.ContributionRepository;
import ma.daret.common.web.ApiException;
import ma.daret.identity.kyc.KycGate;
import ma.daret.identity.user.User;
import ma.daret.identity.user.UserRepository;
import ma.daret.scoring.ScoringStrategy.Outcome;
import ma.daret.scoring.ScoringStrategy.Signals;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Computes and persists {@link CreditScore}s from available signals, delegating the
 * policy to a {@link ScoringStrategy}. Recomputed on demand (e.g. after payment
 * events) and read by matching's score gate.
 */
@Service
public class ScoringService {

    private final CreditScoreRepository scores;
    private final UserRepository users;
    private final KycGate kycGate;
    private final ContributionRepository contributions;
    private final ScoringStrategy strategy;

    public ScoringService(CreditScoreRepository scores,
                          UserRepository users,
                          KycGate kycGate,
                          ContributionRepository contributions,
                          ScoringStrategy strategy) {
        this.scores = scores;
        this.users = users;
        this.kycGate = kycGate;
        this.contributions = contributions;
        this.strategy = strategy;
    }

    @Transactional
    public CreditScore recompute(UUID userId) {
        User user = users.findById(userId)
                .orElseThrow(() -> ApiException.notFound("USER_NOT_FOUND", "User not found"));

        long tenureDays = Duration.between(user.getCreatedAt(), Instant.now()).toDays();
        int paid = contributions.countByUserIdAndStatus(userId, Contribution.Status.PAID);
        int missed = contributions.countByUserIdAndStatus(userId, Contribution.Status.MISSED);

        Outcome outcome = strategy.score(new Signals(kycGate.isVerified(userId), tenureDays, paid, missed));

        CreditScore score = scores.findByUserId(userId).orElseGet(() -> new CreditScore(userId));
        score.setScore(outcome.score());
        score.setTier(outcome.tier());
        score.setFactors(outcome.factors());
        score.setComputedAt(Instant.now());
        return scores.save(score);
    }

    /** Read the stored score, computing one if none exists yet. */
    @Transactional
    public CreditScore getOrCompute(UUID userId) {
        return scores.findByUserId(userId).orElseGet(() -> recompute(userId));
    }

    /** The user's current tier, for the matching score gate. Always fresh. */
    @Transactional
    public CreditTier currentTier(UUID userId) {
        return recompute(userId).getTier();
    }
}
