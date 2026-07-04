package ma.daret.notifications;

import ma.daret.identity.auth.AuthPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** The caller's in-app notification inbox (spec FR-17). */
@RestController
@RequestMapping("/api/v1/me/notifications")
@PreAuthorize("hasRole('MEMBER')")
public class NotificationController {

    private final NotificationService service;

    public NotificationController(NotificationService service) {
        this.service = service;
    }

    public record NotificationView(UUID id, String type, String message, boolean read, Instant at) {
        static NotificationView from(Notification n) {
            return new NotificationView(n.getId(), n.getType(), n.getMessage(), n.isRead(), n.getCreatedAt());
        }
    }

    @GetMapping
    public List<NotificationView> list(@AuthenticationPrincipal AuthPrincipal principal) {
        return service.recent(principal.userId(), 50).stream().map(NotificationView::from).toList();
    }

    @GetMapping("/unread-count")
    public Map<String, Long> unreadCount(@AuthenticationPrincipal AuthPrincipal principal) {
        return Map.of("unread", service.unread(principal.userId()));
    }

    @PostMapping("/read")
    public Map<String, Integer> markAllRead(@AuthenticationPrincipal AuthPrincipal principal) {
        return Map.of("marked", service.markAllRead(principal.userId()));
    }
}
