package ma.daret.social;

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

/** A single-use invite code to join a private (invite-only) circle. */
@Entity
@Table(name = "circle_invites")
@Getter
@Setter
@NoArgsConstructor
public class CircleInvite extends BaseEntity {

    public enum Status { ACTIVE, USED, REVOKED }

    @Column(nullable = false, unique = true)
    private String code;

    @Column(name = "circle_id", nullable = false)
    private UUID circleId;

    @Column(name = "inviter_user_id", nullable = false)
    private UUID inviterUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.ACTIVE;

    public CircleInvite(String code, UUID circleId, UUID inviterUserId) {
        this.code = code;
        this.circleId = circleId;
        this.inviterUserId = inviterUserId;
        this.status = Status.ACTIVE;
    }
}
