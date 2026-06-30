package ma.daret.economics;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ma.daret.common.domain.BaseEntity;
import ma.daret.scoring.CreditTier;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Pricing inputs for the fee curve (spec §5.3, §8). All amounts are in MAD.
 *
 * <ul>
 *   <li>{@code serviceFee} — the platform's flat facilitation fee per member per
 *       circle. Always charged. This is a service charge, never interest.</li>
 *   <li>{@code baseFee} — base input to the per-slot position fee.</li>
 *   <li>{@code redistributionShare} — scales the payout bonus paid to late slots.</li>
 *   <li>{@code tierMultipliers} — per-{@link CreditTier} multiplier applied to the
 *       position fee.</li>
 * </ul>
 */
@Entity
@Table(name = "fee_schedules")
@Getter
@Setter
@NoArgsConstructor
public class FeeSchedule extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String name;

    @Column(name = "service_fee", nullable = false, precision = 15, scale = 2)
    private BigDecimal serviceFee;

    @Column(name = "base_fee", nullable = false, precision = 15, scale = 2)
    private BigDecimal baseFee;

    @Column(name = "redistribution_share", nullable = false, precision = 15, scale = 2)
    private BigDecimal redistributionShare;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tier_multipliers", nullable = false, columnDefinition = "jsonb")
    private Map<String, BigDecimal> tierMultipliers = Map.of();

    /** Multiplier for a tier, defaulting to 1.0 when the tier is absent. */
    public BigDecimal multiplierFor(CreditTier tier) {
        BigDecimal m = tier == null ? null : tierMultipliers.get(tier.name());
        return m != null ? m : BigDecimal.ONE;
    }

    /** Smallest configured multiplier (used to check the platform-margin invariant). */
    public BigDecimal minMultiplier() {
        return tierMultipliers.values().stream()
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ONE);
    }
}
