package ma.daret.risk;

import ma.daret.circles.SlotPreference;
import ma.daret.common.web.ApiException;
import ma.daret.scoring.CreditTier;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Assigns members to slot positions 1..N (spec FR-10, FR-11), honouring:
 * <ul>
 *   <li><b>Score gate:</b> the earliest third of slots (the high-"borrowing"
 *       positions) may be taken only by tier A or B.</li>
 *   <li><b>Preferences:</b> EARLY members trend to low positions, LATE to high.</li>
 * </ul>
 * Returns a per-member rationale. Pure and deterministic — no persistence.
 */
@Service
public class MatchingService {

    /** A member (or gap-fill platform seat) to be placed. */
    public record Candidate(UUID memberId, UUID userId, CreditTier tier,
                            SlotPreference preference, Instant joinedAt, boolean platformFunded) {
    }

    public record Assignment(UUID memberId, UUID userId, int position,
                             boolean platformFunded, String rationale) {
    }

    public List<Assignment> assign(int seatCount, List<Candidate> candidates) {
        if (candidates.size() != seatCount) {
            throw ApiException.conflict("ROSTER_MISMATCH",
                    "Expected " + seatCount + " candidates, got " + candidates.size());
        }
        int gatedCount = (int) Math.ceil(seatCount / 3.0);

        // Early third: only tier A/B, preferring EARLY-wanting, stronger-tier, earlier joiners.
        List<Candidate> eligibleEarly = candidates.stream()
                .filter(c -> c.tier() == CreditTier.A || c.tier() == CreditTier.B)
                .sorted(Comparator
                        .comparingInt((Candidate c) -> prefRank(c.preference()))
                        .thenComparing(c -> c.tier().ordinal())
                        .thenComparing(Candidate::joinedAt))
                .toList();

        if (eligibleEarly.size() < gatedCount) {
            throw ApiException.conflict("MATCH_INFEASIBLE",
                    "Not enough tier A/B members (" + eligibleEarly.size()
                            + ") to fill the earliest " + gatedCount + " slots");
        }

        List<Assignment> result = new ArrayList<>(seatCount);
        List<Candidate> early = eligibleEarly.subList(0, gatedCount);
        for (int i = 0; i < early.size(); i++) {
            Candidate c = early.get(i);
            result.add(new Assignment(c.memberId(), c.userId(), i + 1, c.platformFunded(),
                    earlyRationale(i + 1, c)));
        }

        // Everyone else fills the later slots; LATE preference trends toward the end.
        List<Candidate> remaining = new ArrayList<>(candidates);
        remaining.removeAll(early);
        remaining.sort(Comparator
                .comparingInt((Candidate c) -> prefRank(c.preference()))
                .thenComparing(Candidate::joinedAt));
        for (int i = 0; i < remaining.size(); i++) {
            Candidate c = remaining.get(i);
            int position = gatedCount + 1 + i;
            result.add(new Assignment(c.memberId(), c.userId(), position, c.platformFunded(),
                    laterRationale(position, c)));
        }

        result.sort(Comparator.comparingInt(Assignment::position));
        return result;
    }

    private static int prefRank(SlotPreference p) {
        return switch (p) {
            case EARLY -> 0;
            case FLEXIBLE -> 1;
            case LATE -> 2;
        };
    }

    private static String earlyRationale(int position, Candidate c) {
        String who = c.platformFunded() ? "platform gap-fill" : "tier " + c.tier();
        String pref = c.preference() == SlotPreference.EARLY
                ? "EARLY preference honoured"
                : c.preference() + " preference; placed early to fill the gated third";
        return "slot " + position + ": " + who + " eligible for early third; " + pref;
    }

    private static String laterRationale(int position, Candidate c) {
        if (c.tier() != CreditTier.A && c.tier() != CreditTier.B) {
            return "slot " + position + ": tier " + c.tier()
                    + " restricted to later slots by score gate; " + c.preference() + " preference";
        }
        return "slot " + position + ": " + c.preference() + " preference honoured";
    }
}
