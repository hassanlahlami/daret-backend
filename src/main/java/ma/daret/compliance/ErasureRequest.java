package ma.daret.compliance;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ma.daret.common.domain.BaseEntity;

import java.util.UUID;

/**
 * A member's right-to-erasure request (CNDP / GDPR art. 17). Reviewed by a
 * compliance officer rather than executed automatically, because ledger and
 * obligation records must be retained while a member is active in a circle.
 */
@Entity
@Table(name = "erasure_requests")
@Getter
@Setter
@NoArgsConstructor
public class ErasureRequest extends BaseEntity {

    public enum Status { PENDING, APPROVED, REJECTED }

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(columnDefinition = "text")
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.PENDING;

    @Column(columnDefinition = "text")
    private String resolution;

    public ErasureRequest(UUID userId, String reason) {
        this.userId = userId;
        this.reason = reason;
    }
}
