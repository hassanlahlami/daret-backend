package ma.daret.scoring;

/**
 * Credit tiers A–E (spec FR-5). Drives the per-member position-fee multiplier in
 * a {@code FeeSchedule} and, from Phase 5, the slot eligibility gate. A is the
 * strongest tier; E the weakest.
 */
public enum CreditTier {
    A, B, C, D, E
}
