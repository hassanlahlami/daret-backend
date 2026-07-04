package ma.daret.risk;

import ma.daret.common.web.ApiException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RiskServiceTest {

    // 10% exposure cap, 3-day grace, default after 2 misses, AML threshold 50k.
    private final RiskService risk =
            new RiskService(new RiskProperties(0.10, 3, 2, new java.math.BigDecimal("50000")));

    @Test
    void full_circle_needs_no_gap_fill() {
        RiskService.GapFillPlan plan = risk.planGapFill(5, 5);
        assertThat(plan.emptySeats()).isZero();
    }

    @Test
    void gap_fill_within_cap_is_allowed() {
        RiskService.GapFillPlan plan = risk.planGapFill(10, 9); // 1/10 = 10% == cap
        assertThat(plan.emptySeats()).isEqualTo(1);
        assertThat(plan.exposurePct()).isEqualTo(0.10);
    }

    @Test
    void gap_fill_over_cap_rejects_activation() {
        assertThatThrownBy(() -> risk.planGapFill(10, 8)) // 2/10 = 20% > 10%
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("exceeds");
        // Small circles can't be gap-filled at all under a 10% cap.
        assertThatThrownBy(() -> risk.planGapFill(3, 2)) // 1/3 = 33%
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("exceeds");
    }
}
