package ma.daret.economics;

import ma.daret.common.web.ApiException;
import ma.daret.scoring.CreditTier;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Pure, fully-tested implementation of the position-fee curve (spec §5.3).
 *
 * <pre>
 *   weight(k)      = (N - k) / (N - 1)               k = 1..N  → 1.0 .. 0.0
 *   positionFee(k) = baseFee × weight(k) × tierMultiplier(member)
 *   payoutBonus(k) = redistributionShare × (1 - weight(k))
 *   Invariant: Σ positionFee(k) ≥ Σ payoutBonus(k)
 * </pre>
 *
 * No clock, no persistence, no framing of "interest" — only {@code serviceFee} /
 * {@code positionFee} / {@code payoutBonus}.
 */
@Component
public class FeeEngine {

    private static final int MONEY_SCALE = 2;
    private static final int WEIGHT_SCALE = 6;

    /** Positioning weight for slot {@code position} in a circle of {@code seatCount} seats. */
    public BigDecimal weight(int position, int seatCount) {
        requireValidPosition(position, seatCount);
        if (seatCount == 1) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(seatCount - position)
                .divide(BigDecimal.valueOf(seatCount - 1L), WEIGHT_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Price one slot. In Sharia mode the position fee and payout bonus are zero;
     * only the flat service fee remains.
     */
    public SlotEconomics priceSlot(FeeSchedule schedule, int seatCount, int position,
                                   CreditTier tier, boolean shariaMode) {
        BigDecimal w = weight(position, seatCount);

        BigDecimal positionFee;
        BigDecimal payoutBonus;
        if (shariaMode) {
            positionFee = BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
            payoutBonus = BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        } else {
            positionFee = schedule.getBaseFee()
                    .multiply(w)
                    .multiply(schedule.multiplierFor(tier))
                    .setScale(MONEY_SCALE, RoundingMode.HALF_UP);
            payoutBonus = schedule.getRedistributionShare()
                    .multiply(BigDecimal.ONE.subtract(w))
                    .setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        }

        BigDecimal serviceFee = schedule.getServiceFee().setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        return new SlotEconomics(position, w, positionFee, payoutBonus, serviceFee);
    }

    /**
     * Validate the platform-margin invariant for a schedule: summed over all slots,
     * position fees must cover payout bonuses even in the worst (lowest-multiplier)
     * case. Since Σ weight = Σ (1 − weight) = N/2, this reduces to
     * {@code baseFee × minMultiplier ≥ redistributionShare}.
     */
    public void assertMarginInvariant(FeeSchedule schedule) {
        BigDecimal minPositionFactor = schedule.getBaseFee().multiply(schedule.minMultiplier());
        if (minPositionFactor.compareTo(schedule.getRedistributionShare()) < 0) {
            throw ApiException.badRequest("FEE_MARGIN_VIOLATION",
                    "baseFee × minMultiplier must be ≥ redistributionShare so Σ positionFee ≥ Σ payoutBonus");
        }
    }

    private static void requireValidPosition(int position, int seatCount) {
        if (position < 1 || position > seatCount) {
            throw ApiException.badRequest("INVALID_POSITION",
                    "position must be between 1 and " + seatCount);
        }
    }
}
