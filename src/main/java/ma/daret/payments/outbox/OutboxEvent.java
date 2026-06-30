package ma.daret.payments.outbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ma.daret.common.domain.BaseEntity;

import java.time.Instant;

/**
 * Transactional-outbox record (spec §10.5). Written in the same transaction as the
 * business state change, then relayed to the payment port after commit — so no
 * financial event is lost on crash. Delivery is at-least-once; handlers are idempotent.
 */
@Entity
@Table(name = "outbox_events")
@Getter
@Setter
@NoArgsConstructor
public class OutboxEvent extends BaseEntity {

    public enum Status { PENDING, PROCESSED, FAILED }

    /** e.g. COLLECT_CONTRIBUTION, DISBURSE_PAYOUT. */
    @Column(nullable = false)
    private String type;

    /** Opaque payload (JSON) the handler needs — typically the target entity id. */
    @Column(nullable = false, columnDefinition = "text")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.PENDING;

    @Column(nullable = false)
    private int attempts = 0;

    @Column(name = "processed_at")
    private Instant processedAt;

    public OutboxEvent(String type, String payload) {
        this.type = type;
        this.payload = payload;
        this.status = Status.PENDING;
    }
}
