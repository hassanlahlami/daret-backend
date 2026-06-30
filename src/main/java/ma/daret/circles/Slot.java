package ma.daret.circles;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ma.daret.common.domain.BaseEntity;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * A priced position {@code 1..N} in a circle (spec §8). Created at activation with
 * its position fee / payout bonus from the {@link ma.daret.economics.FeeEngine}.
 * {@code platformFunded} marks a slot the platform gap-filled (Phase 5).
 */
@Entity
@Table(name = "slots", uniqueConstraints =
        @UniqueConstraint(name = "uq_slot_position", columnNames = {"circle_id", "position"}))
@Getter
@Setter
@NoArgsConstructor
public class Slot extends BaseEntity {

    @Column(name = "circle_id", nullable = false)
    private UUID circleId;

    @Column(nullable = false)
    private int position;

    @Column(name = "position_fee", nullable = false, precision = 15, scale = 2)
    private BigDecimal positionFee = BigDecimal.ZERO;

    @Column(name = "payout_bonus", nullable = false, precision = 15, scale = 2)
    private BigDecimal payoutBonus = BigDecimal.ZERO;

    @Column(name = "platform_funded", nullable = false)
    private boolean platformFunded = false;

    /** The member occupying this slot. */
    @Column(name = "user_id", nullable = false)
    private UUID userId;
}
