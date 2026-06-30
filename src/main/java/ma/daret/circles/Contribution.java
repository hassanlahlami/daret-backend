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
import java.time.LocalDate;
import java.util.UUID;

/**
 * A member's payment obligation for one cycle (spec §8, §9.2):
 * {@code SCHEDULED → PAID} (on settle) · {@code SCHEDULED → MISSED} (grace expiry)
 * · {@code MISSED → PAID} (late recovery).
 */
@Entity
@Table(name = "contributions", uniqueConstraints =
        @UniqueConstraint(name = "uq_contribution", columnNames = {"circle_member_id", "cycle_index"}))
@Getter
@Setter
@NoArgsConstructor
public class Contribution extends BaseEntity {

    public enum Status { SCHEDULED, PAID, MISSED }

    @Column(name = "circle_member_id", nullable = false)
    private UUID circleMemberId;

    @Column(name = "circle_id", nullable = false)
    private UUID circleId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "cycle_index", nullable = false)
    private int cycleIndex;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.SCHEDULED;

    @Column(name = "payment_intent_id")
    private UUID paymentIntentId;
}
