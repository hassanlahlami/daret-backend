package ma.daret.payments;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ma.daret.common.domain.BaseEntity;
import ma.daret.payments.port.PaymentDirection;
import ma.daret.payments.port.PaymentStatus;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * A single money movement through the {@code PaymentProvider} (spec §8). The only
 * entity that touches money. {@code idempotencyKey} guarantees a contribution or
 * payout is never charged twice across retries.
 */
@Entity
@Table(name = "payment_intents")
@Getter
@Setter
@NoArgsConstructor
public class PaymentIntent extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentDirection direction;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "external_ref")
    private String externalRef;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(name = "idempotency_key", nullable = false, unique = true)
    private String idempotencyKey;

    /** What this intent settles: CONTRIBUTION or PAYOUT. */
    @Column(name = "ref_type", nullable = false)
    private String refType;

    @Column(name = "ref_id", nullable = false)
    private UUID refId;
}
