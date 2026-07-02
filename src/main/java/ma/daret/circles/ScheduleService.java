package ma.daret.circles;

import ma.daret.circles.ScheduleDtos.CircleScheduleView;
import ma.daret.circles.ScheduleDtos.ContributionView;
import ma.daret.circles.ScheduleDtos.PayoutView;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class ScheduleService {

    private final ContributionRepository contributions;
    private final PayoutRepository payouts;

    public ScheduleService(ContributionRepository contributions, PayoutRepository payouts) {
        this.contributions = contributions;
        this.payouts = payouts;
    }

    @Transactional(readOnly = true)
    public CircleScheduleView circleSchedule(UUID circleId) {
        List<ContributionView> cs = contributions.findByCircleId(circleId).stream()
                .sorted(Comparator.comparingInt(Contribution::getCycleIndex))
                .map(ContributionView::from)
                .toList();
        List<PayoutView> ps = payouts.findByCircleId(circleId).stream()
                .sorted(Comparator.comparingInt(Payout::getCycleIndex))
                .map(PayoutView::from)
                .toList();
        return new CircleScheduleView(circleId, cs, ps);
    }

    /** A member's still-outstanding (SCHEDULED) contributions. */
    @Transactional(readOnly = true)
    public List<ContributionView> obligations(UUID userId) {
        return contributions.findByUserIdAndStatus(userId, Contribution.Status.SCHEDULED).stream()
                .sorted(Comparator.comparing(Contribution::getDueDate))
                .map(ContributionView::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PayoutView> payoutsForUser(UUID userId) {
        return payouts.findByRecipientUserId(userId).stream()
                .map(PayoutView::from)
                .toList();
    }
}
