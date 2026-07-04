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
 * An AML case raised by the rule engine for compliance review (spec FR-19).
 */
@Entity
@Table(name = "aml_flags")
@Getter
@Setter
@NoArgsConstructor
public class AmlFlag extends BaseEntity {

    public enum Type { MEMBER_DEFAULT, LARGE_MOVEMENT, VELOCITY }

    public enum Severity { LOW, MEDIUM, HIGH }

    public enum Status { OPEN, CLEARED }

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "circle_id")
    private UUID circleId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Type type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Severity severity = Severity.MEDIUM;

    @Column(nullable = false, columnDefinition = "text")
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.OPEN;
}
