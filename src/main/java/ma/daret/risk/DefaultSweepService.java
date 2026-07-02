package ma.daret.risk;

import ma.daret.circles.CircleMember;
import ma.daret.circles.CircleMemberRepository;
import ma.daret.circles.Contribution;
import ma.daret.circles.ContributionRepository;
import ma.daret.circles.MemberStanding;
import ma.daret.notifications.NotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Sweeps overdue contributions (spec FR-15): once past the grace window a
 * SCHEDULED contribution becomes MISSED, its member is marked LATE, and — after
 * {@code defaultAfterMissed} misses — DEFAULTED. Notifications fire at each step.
 */
@Service
public class DefaultSweepService {

    private final ContributionRepository contributions;
    private final CircleMemberRepository members;
    private final NotificationService notifications;
    private final RiskProperties props;

    public DefaultSweepService(ContributionRepository contributions,
                               CircleMemberRepository members,
                               NotificationService notifications,
                               RiskProperties props) {
        this.contributions = contributions;
        this.members = members;
        this.notifications = notifications;
        this.props = props;
    }

    public record SweepResult(int missedMarked, int membersLate, int membersDefaulted) {
    }

    @Transactional
    public SweepResult sweep(LocalDate asOf) {
        LocalDate cutoff = asOf.minusDays(props.contributionGraceDays());
        List<Contribution> overdue = contributions
                .findByStatusAndDueDateLessThanEqual(Contribution.Status.SCHEDULED, cutoff);

        Set<UUID> affectedMembers = new HashSet<>();
        for (Contribution c : overdue) {
            c.setStatus(Contribution.Status.MISSED);
            contributions.save(c);
            affectedMembers.add(c.getCircleMemberId());
            notifications.notify(c.getUserId(), "CONTRIBUTION_MISSED",
                    "Contribution for cycle " + c.getCycleIndex() + " was missed");
        }

        int late = 0;
        int defaulted = 0;
        for (UUID memberId : affectedMembers) {
            CircleMember member = members.findById(memberId).orElse(null);
            if (member == null) {
                continue;
            }
            int missed = contributions.countByCircleMemberIdAndStatus(memberId, Contribution.Status.MISSED);
            if (missed >= props.defaultAfterMissed()) {
                member.setStanding(MemberStanding.DEFAULTED);
                defaulted++;
                notifications.notify(member.getUserId(), "MEMBER_DEFAULTED",
                        "Marked as defaulted after " + missed + " missed contributions");
            } else {
                member.setStanding(MemberStanding.LATE);
                late++;
                notifications.notify(member.getUserId(), "MEMBER_LATE",
                        "Marked as late; please settle your contribution");
            }
            members.save(member);
        }
        return new SweepResult(overdue.size(), late, defaulted);
    }
}
