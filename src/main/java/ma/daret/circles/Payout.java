package ma.daret.circles;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ma.daret.common.domain.BaseEntity;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * The pot disbursed to a cycle's recipient (spec §8, FR-14). Exactly one payout
 * per member across the circle's life — enforced by a unique constraint on
 * {@code (circle_id, recipient_member_id)} and again by the ledger.
 */
@Entity
@Table(name = "payouts", uniqueConstraints = {
        @UniqueConstraint(name = "uq_payout_member", columnNames = {"circle_id", "recipient_member_id"}),
        @UniqueConstraint(name = "uq_payout_cycle", columnNames = {"circle_id", "cycle_index"})
})
@Getter
@Setter
@NoArgsConstructor
public class Payout extends BaseEntity {

    public enum Status { SCHEDULED, PAID, FAILED }

    @Column(name = "circle_id", nullable = false)
    private UUID circleId;

    @Column(name = "recipient_member_id", nullable = false)
    private UUID recipientMemberId;

    @Column(name = "recipient_user_id", nullable = false)
    private UUID recipientUserId;

    @Column(name = "cycle_index", nullable = false)
    private int cycleIndex;

    /** Pot adjusted by the recipient's position economics (pot + payoutBonus − fees). */
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    /** Platform's cut for this payout: service fee + (position fee − payout bonus). */
    @Column(name = "platform_cut", nullable = false, precision = 15, scale = 2)
    private BigDecimal platformCut = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.SCHEDULED;

    @Column(name = "payment_intent_id")
    private UUID paymentIntentId;
}
