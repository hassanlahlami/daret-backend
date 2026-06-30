package ma.daret.circles;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ma.daret.common.domain.BaseEntity;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * A daret: a fixed group of {@code seatCount} members each contributing
 * {@code contributionAmount} (MAD) every {@code cycleLengthDays}, with one member
 * receiving the pot each cycle. The platform never holds these funds — this row
 * stores the circle's terms and lifecycle state only.
 */
@Entity
@Table(name = "circles")
@Getter
@Setter
@NoArgsConstructor
public class Circle extends BaseEntity {

    @Column(nullable = false)
    private String name;

    /** Per-member, per-cycle contribution in MAD. */
    @Column(name = "contribution_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal contributionAmount;

    @Column(name = "seat_count", nullable = false)
    private int seatCount;

    @Column(name = "cycle_length_days", nullable = false)
    private int cycleLengthDays;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CircleStatus status = CircleStatus.FORMING;

    /** When true, position fees are disabled — pure rotation, interest-free (spec §5.2). */
    @Column(name = "sharia_mode", nullable = false)
    private boolean shariaMode = true;

    /** Pricing schedule; populated in Phase 3 (economics). Nullable for now. */
    @Column(name = "fee_schedule_id")
    private UUID feeScheduleId;

    /** The member who created the circle. */
    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;
}
