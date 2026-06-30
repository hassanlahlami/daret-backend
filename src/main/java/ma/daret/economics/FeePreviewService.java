package ma.daret.economics;

import ma.daret.circles.Circle;
import ma.daret.circles.CircleRepository;
import ma.daret.common.web.ApiException;
import ma.daret.economics.FeePreviewDtos.FeePreviewResponse;
import ma.daret.economics.FeePreviewDtos.ObligationLine;
import ma.daret.scoring.CreditTier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Composes a member-facing fee/payout preview for a chosen slot in a circle
 * (spec FR-9): the position fee/bonus, the projected payout date, and the full
 * per-cycle obligation schedule — all shown before the member commits.
 */
@Service
public class FeePreviewService {

    private final CircleRepository circles;
    private final FeeScheduleRepository feeSchedules;
    private final FeeEngine feeEngine;

    public FeePreviewService(CircleRepository circles,
                             FeeScheduleRepository feeSchedules,
                             FeeEngine feeEngine) {
        this.circles = circles;
        this.feeSchedules = feeSchedules;
        this.feeEngine = feeEngine;
    }

    /**
     * @param tier the member's credit tier for position-fee pricing; until scoring
     *             exists (Phase 5) callers pass a neutral tier. Ignored in Sharia mode.
     */
    @Transactional(readOnly = true)
    public FeePreviewResponse preview(UUID circleId, int position, CreditTier tier) {
        Circle circle = circles.findById(circleId)
                .orElseThrow(() -> ApiException.notFound("CIRCLE_NOT_FOUND", "Circle not found"));

        UUID scheduleId = circle.getFeeScheduleId() != null
                ? circle.getFeeScheduleId() : FeeScheduleRepository.DEFAULT_ID;
        FeeSchedule schedule = feeSchedules.findById(scheduleId)
                .orElseThrow(() -> ApiException.notFound("FEE_SCHEDULE_NOT_FOUND", "Fee schedule not found"));

        int seats = circle.getSeatCount();
        SlotEconomics econ = feeEngine.priceSlot(schedule, seats, position, tier, circle.isShariaMode());

        BigDecimal contribution = circle.getContributionAmount();
        BigDecimal pot = contribution.multiply(BigDecimal.valueOf(seats));
        BigDecimal totalContributions = pot; // one contribution per cycle over N cycles

        // Projection from today (circle not yet active): cycle i is due i-1 cadences out.
        LocalDate start = LocalDate.now();
        List<ObligationLine> scheduleLines = new ArrayList<>(seats);
        for (int cycle = 1; cycle <= seats; cycle++) {
            LocalDate dueDate = start.plusDays((long) (cycle - 1) * circle.getCycleLengthDays());
            scheduleLines.add(new ObligationLine(cycle, dueDate, contribution, cycle == position));
        }
        LocalDate projectedPayoutDate = start.plusDays((long) (position - 1) * circle.getCycleLengthDays());

        return new FeePreviewResponse(
                position,
                position,
                projectedPayoutDate,
                pot,
                contribution,
                totalContributions,
                econ.serviceFee(),
                econ.positionFee(),
                econ.payoutBonus(),
                econ.netAdjustment(),
                circle.isShariaMode(),
                scheduleLines
        );
    }
}
