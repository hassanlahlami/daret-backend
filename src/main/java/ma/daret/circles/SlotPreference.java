package ma.daret.circles;

/**
 * A member's payout-timing preference, expressed at join time (spec FR-8).
 * The matching engine (Phase 5) turns these into concrete slot positions.
 *   EARLY = wants to receive the pot sooner (a "borrowing" position);
 *   LATE  = happy to wait (a "saving" position);
 *   FLEXIBLE = no strong preference.
 */
public enum SlotPreference {
    EARLY,
    FLEXIBLE,
    LATE
}
