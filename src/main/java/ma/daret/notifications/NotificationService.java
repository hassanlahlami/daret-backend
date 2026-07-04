package ma.daret.notifications;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Channel-agnostic member notifications (spec FR-17). Persists to an in-app inbox
 * and logs; SMS / web-push adapters plug in behind this same call site.
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository repository;

    public NotificationService(NotificationRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void notify(UUID userId, String type, String message) {
        repository.save(new Notification(userId, type, message));
        log.info("notify user={} type={} :: {}", userId, type, message);
    }

    @Transactional(readOnly = true)
    public List<Notification> recent(UUID userId, int limit) {
        return repository.findByUserIdOrderByCreatedAtDesc(userId, Limit.of(limit));
    }

    @Transactional(readOnly = true)
    public long unread(UUID userId) {
        return repository.countByUserIdAndReadFalse(userId);
    }

    @Transactional
    public int markAllRead(UUID userId) {
        return repository.markAllRead(userId);
    }
}
