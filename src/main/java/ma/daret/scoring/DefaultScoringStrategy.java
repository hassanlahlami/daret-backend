package ma.daret.scoring;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Default scoring policy for a thin-data market (spec FR-5). Weighted toward
 * on-platform behaviour; a brand-new verified user lands conservatively (~tier C),
 * with payment history the biggest mover once it exists.
 *
 * <pre>
 *   base            300
 *   kyc verified   +200
 *   tenure         +150  (linear to 1 year)
 *   payment        +350 × paidRatio  (neutral prior 150 when no history)
 * </pre>
 *
 * A brand-new verified user therefore starts at exactly tier B (650) — eligible for
 * early "borrowing" slots — and missed contributions pull the score down into C–E,
 * which the matching score gate then keeps out of the earliest slots.
 */
@Component
public class DefaultScoringStrategy implements ScoringStrategy {

    private static final int BASE = 300;
    private static final int KYC_WEIGHT = 200;
    private static final int TENURE_WEIGHT = 150;
    private static final long TENURE_FULL_DAYS = 365;
    private static final int PAYMENT_WEIGHT = 350;
    private static final int PAYMENT_NEUTRAL_PRIOR = 150;
    private static final int VOUCH_WEIGHT = 25;   // per vouch
    private static final int VOUCH_CAP = 150;      // max trust bonus

    @Override
    public Outcome score(Signals s) {
        Map<String, BigDecimal> factors = new LinkedHashMap<>();
        factors.put("base", BigDecimal.valueOf(BASE));

        int kyc = s.kycVerified() ? KYC_WEIGHT : 0;
        factors.put("kyc", BigDecimal.valueOf(kyc));

        long cappedTenure = Math.min(Math.max(s.tenureDays(), 0), TENURE_FULL_DAYS);
        int tenure = (int) Math.round((double) cappedTenure / TENURE_FULL_DAYS * TENURE_WEIGHT);
        factors.put("tenure", BigDecimal.valueOf(tenure));

        int total = s.contributionsPaid() + s.contributionsMissed();
        int payment = total == 0
                ? PAYMENT_NEUTRAL_PRIOR
                : (int) Math.round((double) s.contributionsPaid() / total * PAYMENT_WEIGHT);
        factors.put("paymentHistory", BigDecimal.valueOf(payment));

        int vouch = (int) Math.min(VOUCH_CAP, Math.max(0, s.vouchesReceived()) * VOUCH_WEIGHT);
        factors.put("vouches", BigDecimal.valueOf(vouch));

        int score = Math.min(1000, Math.max(0, BASE + kyc + tenure + payment + vouch));
        factors.put("total", BigDecimal.valueOf(score));

        return new Outcome(score, tierFor(score), factors);
    }

    private static CreditTier tierFor(int score) {
        if (score >= 800) return CreditTier.A;
        if (score >= 650) return CreditTier.B;
        if (score >= 500) return CreditTier.C;
        if (score >= 350) return CreditTier.D;
        return CreditTier.E;
    }

    /** Exposed so callers can round-trip a stored score to its tier consistently. */
    public static CreditTier tierOf(int score) {
        return tierFor(score);
    }
}
