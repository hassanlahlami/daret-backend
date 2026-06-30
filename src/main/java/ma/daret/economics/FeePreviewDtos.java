package ma.daret.economics;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Fee-preview response payloads (spec FR-9). All amounts MAD; never "interest". */
public final class FeePreviewDtos {

    private FeePreviewDtos() {
    }

    public record ObligationLine(
            int cycleIndex,
            LocalDate dueDate,
            BigDecimal amount,
            boolean payoutCycle
    ) {
    }

    public record FeePreviewResponse(
            int position,
            int payoutCycleIndex,
            LocalDate projectedPayoutDate,
            BigDecimal potAmount,
            BigDecimal contributionPerCycle,
            BigDecimal totalContributions,
            BigDecimal serviceFee,
            BigDecimal positionFee,
            BigDecimal payoutBonus,
            BigDecimal netAdjustmentVsRotation,
            boolean shariaMode,
            List<ObligationLine> schedule
    ) {
    }
}
