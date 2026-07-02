package ma.daret.risk;

import ma.daret.circles.SlotPreference;
import ma.daret.common.web.ApiException;
import ma.daret.risk.MatchingService.Assignment;
import ma.daret.risk.MatchingService.Candidate;
import ma.daret.scoring.CreditTier;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MatchingServiceTest {

    private final MatchingService matching = new MatchingService();

    private Candidate cand(CreditTier tier, SlotPreference pref, int order) {
        UUID id = UUID.randomUUID();
        return new Candidate(id, id, tier, pref, Instant.ofEpochSecond(order), false);
    }

    @Test
    void score_gate_keeps_lower_tiers_out_of_the_earliest_third() {
        // N=6 → earliest third = 2 gated slots (positions 1,2) require tier A/B.
        List<Candidate> candidates = List.of(
                cand(CreditTier.C, SlotPreference.EARLY, 1),  // wants early but too low a tier
                cand(CreditTier.A, SlotPreference.EARLY, 2),
                cand(CreditTier.B, SlotPreference.FLEXIBLE, 3),
                cand(CreditTier.D, SlotPreference.FLEXIBLE, 4),
                cand(CreditTier.E, SlotPreference.LATE, 5),
                cand(CreditTier.C, SlotPreference.LATE, 6)
        );
        List<Assignment> result = matching.assign(6, candidates);
        Map<UUID, Integer> pos = result.stream()
                .collect(Collectors.toMap(Assignment::userId, Assignment::position));
        Map<UUID, CreditTier> tierByUser = candidates.stream()
                .collect(Collectors.toMap(Candidate::userId, Candidate::tier));

        // Gated slots (1,2) hold only A/B.
        result.stream().filter(a -> a.position() <= 2)
                .forEach(a -> assertThat(tierByUser.get(a.userId())).isIn(CreditTier.A, CreditTier.B));
        // The EARLY-preferring C member is pushed past the gate.
        UUID earlyC = candidates.get(0).userId();
        assertThat(pos.get(earlyC)).isGreaterThan(2);
    }

    @Test
    void late_preference_trends_to_the_final_slot() {
        List<Candidate> candidates = List.of(
                cand(CreditTier.A, SlotPreference.EARLY, 1),
                cand(CreditTier.B, SlotPreference.FLEXIBLE, 2),
                cand(CreditTier.C, SlotPreference.LATE, 3)
        );
        List<Assignment> result = matching.assign(3, candidates);
        UUID lateUser = candidates.get(2).userId();
        int lastPos = result.stream().mapToInt(Assignment::position).max().orElseThrow();
        int latePos = result.stream().filter(a -> a.userId().equals(lateUser))
                .findFirst().orElseThrow().position();
        assertThat(latePos).isEqualTo(lastPos);
    }

    @Test
    void rejects_when_too_few_high_tier_members_for_the_gate() {
        // N=3 → 1 gated slot needs A/B, but everyone is C/D.
        List<Candidate> candidates = List.of(
                cand(CreditTier.C, SlotPreference.EARLY, 1),
                cand(CreditTier.D, SlotPreference.FLEXIBLE, 2),
                cand(CreditTier.C, SlotPreference.LATE, 3)
        );
        assertThatThrownBy(() -> matching.assign(3, candidates))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Not enough tier A/B");
    }
}
