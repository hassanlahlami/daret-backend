package ma.daret.notifications;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Channel-agnostic member notifications (spec FR-17). MVP logs the event; SMS /
 * push adapters plug in later behind this same call site.
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    public void notify(UUID userId, String type, String message) {
        log.info("notify user={} type={} :: {}", userId, type, message);
    }
}
