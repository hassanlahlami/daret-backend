package ma.daret.economics;

import ma.daret.common.web.ApiException;
import ma.daret.scoring.CreditTier;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FeeEngineTest {

    private final FeeEngine engine = new FeeEngine();

    private FeeSchedule defaultSchedule() {
        FeeSchedule s = new FeeSchedule();
        s.setName("TEST");
        s.setServiceFee(new BigDecimal("25.00"));
        s.setBaseFee(new BigDecimal("50.00"));
        s.setRedistributionShare(new BigDecimal("40.00"));
        s.setTierMultipliers(Map.of(
                "A", new BigDecimal("1.0"), "B", new BigDecimal("1.15"),
                "C", new BigDecimal("1.35"), "D", new BigDecimal("1.6"),
                "E", new BigDecimal("2.0")));
        return s;
    }

    @Test
    void weight_declines_linearly_from_first_to_last_slot() {
        assertThat(engine.weight(1, 5)).isEqualByComparingTo("1.0");   // earliest
        assertThat(engine.weight(3, 5)).isEqualByComparingTo("0.5");   // middle
        assertThat(engine.weight(5, 5)).isEqualByComparingTo("0.0");   // latest
    }

    @Test
    void sharia_mode_zeroes_position_fee_and_bonus_keeping_service_fee() {
        SlotEconomics econ = engine.priceSlot(defaultSchedule(), 5, 1, CreditTier.C, true);
        assertThat(econ.positionFee()).isEqualByComparingTo("0.00");
        assertThat(econ.payoutBonus()).isEqualByComparingTo("0.00");
        assertThat(econ.serviceFee()).isEqualByComparingTo("25.00");
    }

    @Test
    void early_slot_pays_more_position_fee_than_late_slot() {
        FeeSchedule s = defaultSchedule();
        SlotEconomics early = engine.priceSlot(s, 5, 1, CreditTier.A, false);
        SlotEconomics late = engine.priceSlot(s, 5, 5, CreditTier.A, false);

        // slot 1: weight 1.0 → fee 50*1.0*1.0 = 50.00, bonus 40*(1-1)=0
        assertThat(early.positionFee()).isEqualByComparingTo("50.00");
        assertThat(early.payoutBonus()).isEqualByComparingTo("0.00");
        // slot 5: weight 0.0 → fee 0, bonus 40*(1-0)=40.00
        assertThat(late.positionFee()).isEqualByComparingTo("0.00");
        assertThat(late.payoutBonus()).isEqualByComparingTo("40.00");
        assertThat(early.positionFee()).isGreaterThan(late.positionFee());
    }

    @Test
    void platform_margin_invariant_holds_across_all_slots() {
        FeeSchedule s = defaultSchedule();
        int n = 7;
        BigDecimal totalFees = BigDecimal.ZERO;
        BigDecimal totalBonuses = BigDecimal.ZERO;
        for (int k = 1; k <= n; k++) {
            SlotEconomics e = engine.priceSlot(s, n, k, CreditTier.A, false);
            totalFees = totalFees.add(e.positionFee());
            totalBonuses = totalBonuses.add(e.payoutBonus());
        }
        assertThat(totalFees).isGreaterThanOrEqualTo(totalBonuses);
    }

    @Test
    void assertMarginInvariant_rejects_bonus_exceeding_fee_capacity() {
        FeeSchedule bad = defaultSchedule();
        bad.setBaseFee(new BigDecimal("10.00"));        // 10 * 1.0 < 40
        assertThatThrownBy(() -> engine.assertMarginInvariant(bad))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("redistributionShare");
    }

    @Test
    void invalid_position_is_rejected() {
        assertThatThrownBy(() -> engine.weight(0, 5)).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> engine.weight(6, 5)).isInstanceOf(ApiException.class);
    }
}
