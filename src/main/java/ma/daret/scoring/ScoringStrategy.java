package ma.daret.scoring;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Pluggable scoring policy (spec FR-5: "scoring is a tunable strategy"). Swap the
 * implementation to change how signals map to a score without touching callers.
 */
public interface ScoringStrategy {

    /** Signals available for scoring in a thin-data market. */
    record Signals(boolean kycVerified, long tenureDays, int contributionsPaid, int contributionsMissed,
                   long vouchesReceived) {
    }

    /** Result: the 0–1000 score, its tier, and the per-factor breakdown. */
    record Outcome(int score, CreditTier tier, Map<String, BigDecimal> factors) {
    }

    Outcome score(Signals signals);
}
