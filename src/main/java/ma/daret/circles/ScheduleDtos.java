package ma.daret.circles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Read-model payloads for schedules, obligations, and payouts. */
public final class ScheduleDtos {

    private ScheduleDtos() {
    }

    public record ContributionView(
            UUID id,
            UUID circleId,
            int cycleIndex,
            BigDecimal amount,
            LocalDate dueDate,
            Contribution.Status status
    ) {
        static ContributionView from(Contribution c) {
            return new ContributionView(c.getId(), c.getCircleId(), c.getCycleIndex(),
                    c.getAmount(), c.getDueDate(), c.getStatus());
        }
    }

    public record PayoutView(
            UUID id,
            UUID circleId,
            int cycleIndex,
            BigDecimal amount,
            Payout.Status status
    ) {
        static PayoutView from(Payout p) {
            return new PayoutView(p.getId(), p.getCircleId(), p.getCycleIndex(), p.getAmount(), p.getStatus());
        }
    }

    public record CircleScheduleView(
            UUID circleId,
            List<ContributionView> contributions,
            List<PayoutView> payouts
    ) {
    }
}
