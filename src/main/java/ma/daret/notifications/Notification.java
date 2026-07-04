package ma.daret.notifications;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ma.daret.common.domain.BaseEntity;

import java.util.UUID;

/** A persisted in-app notification (spec FR-17). */
@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
public class Notification extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false, columnDefinition = "text")
    private String message;

    @Column(nullable = false)
    private boolean read = false;

    public Notification(UUID userId, String type, String message) {
        this.userId = userId;
        this.type = type;
        this.message = message;
    }
}
