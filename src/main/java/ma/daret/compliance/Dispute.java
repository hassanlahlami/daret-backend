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

/** A member-raised dispute/grievance, reviewed by compliance (spec FR-19 / E6). */
@Entity
@Table(name = "disputes")
@Getter
@Setter
@NoArgsConstructor
public class Dispute extends BaseEntity {

    public enum Status { OPEN, RESOLVED }

    @Column(name = "raiser_user_id", nullable = false)
    private UUID raiserUserId;

    @Column(name = "circle_id")
    private UUID circleId;

    @Column(nullable = false)
    private String subject;

    @Column(nullable = false, columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.OPEN;

    @Column(columnDefinition = "text")
    private String resolution;

    public Dispute(UUID raiserUserId, UUID circleId, String subject, String description) {
        this.raiserUserId = raiserUserId;
        this.circleId = circleId;
        this.subject = subject;
        this.description = description;
    }
}
