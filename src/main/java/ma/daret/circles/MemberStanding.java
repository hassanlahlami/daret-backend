package ma.daret.circles;

/**
 * A member's payment standing within a circle (spec §8). Drives risk handling
 * in later phases; every member starts {@code GOOD}.
 */
public enum MemberStanding {
    GOOD,
    LATE,
    DEFAULTED
}
