package ma.daret.ledger;

import java.util.UUID;

/**
 * Canonical account-name builders. Accounts represent claims/positions, not
 * platform cash. Naming is stable so balances stay reconcilable over time.
 */
public final class LedgerAccounts {

    private LedgerAccounts() {
    }

    /** A circle's pot clearing account (rises on collection, empties on payout). */
    public static String circlePot(UUID circleId) {
        return "circle:" + circleId + ":pot";
    }

    /** A member's net position (contributions paid vs. payout received). */
    public static String member(UUID userId) {
        return "member:" + userId;
    }

    /** Platform revenue: service fees plus net position-fee margin. */
    public static final String PLATFORM_REVENUE = "platform:revenue";
}
