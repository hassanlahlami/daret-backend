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

import java.util.UUID;

/**
 * A member's seat in a circle. {@code slotPosition} stays null until the matching
 * engine assigns positions at activation (Phase 5).
 */
@Entity
@Table(name = "circle_members", uniqueConstraints =
        @UniqueConstraint(name = "uq_circle_member", columnNames = {"circle_id", "user_id"}))
@Getter
@Setter
@NoArgsConstructor
public class CircleMember extends BaseEntity {

    @Column(name = "circle_id", nullable = false)
    private UUID circleId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "slot_preference", nullable = false)
    private SlotPreference slotPreference = SlotPreference.FLEXIBLE;

    /** Assigned 1..N by the matching engine at activation; null while FORMING. */
    @Column(name = "slot_position")
    private Integer slotPosition;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MemberStanding standing = MemberStanding.GOOD;

    public CircleMember(UUID circleId, UUID userId, SlotPreference slotPreference) {
        this.circleId = circleId;
        this.userId = userId;
        this.slotPreference = slotPreference;
        this.standing = MemberStanding.GOOD;
    }
}
