package ma.daret.compliance;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ma.daret.common.domain.BaseEntity;

/**
 * A tamper-evident audit record (spec §8, §13). Events form a hash chain:
 * {@code hash = SHA-256(prevHash + seq + actor + action + target + payload)}.
 * Append-only — never updated or deleted.
 */
@Entity
@Table(name = "audit_events")
@Getter
@Setter
@NoArgsConstructor
public class AuditEvent extends BaseEntity {

    /** Monotonic position in the chain (1-based). */
    @Column(nullable = false, unique = true)
    private long seq;

    /** Who performed the action (user id, or "anonymous"/"system"). */
    @Column(nullable = false)
    private String actor;

    @Column(nullable = false)
    private String action;

    @Column
    private String target;

    /** PII-masked JSON payload of the action's inputs. */
    @Column(nullable = false, columnDefinition = "text")
    private String payload;

    @Column(name = "prev_hash", nullable = false)
    private String prevHash;

    @Column(nullable = false, unique = true)
    private String hash;
}
