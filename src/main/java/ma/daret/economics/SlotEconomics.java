package ma.daret.economics;

import java.math.BigDecimal;

/**
 * The priced economics of a single slot position (spec §5.3):
 * its positioning {@code weight}, the {@code positionFee} an early taker pays,
 * the {@code payoutBonus} a late waiter earns, and the always-on {@code serviceFee}.
 * In Sharia mode {@code positionFee} and {@code payoutBonus} are zero.
 */
public record SlotEconomics(
        int position,
        BigDecimal weight,
        BigDecimal positionFee,
        BigDecimal payoutBonus,
        BigDecimal serviceFee
) {
    /** Net adjustment to the member vs. a pure rotation: bonus earned − fees paid. */
    public BigDecimal netAdjustment() {
        return payoutBonus.subtract(positionFee).subtract(serviceFee);
    }
}
