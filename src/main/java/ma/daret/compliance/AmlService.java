package ma.daret.compliance;

import ma.daret.notifications.NotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * AML rule engine (spec §4.7, FR-19). Raises reviewable {@link AmlFlag} cases from
 * platform events (defaults, large movements, velocity). De-duplicates open flags
 * of the same (user, circle, type) so a repeated trigger doesn't spam the queue.
 */
@Service
public class AmlService {

    private final AmlFlagRepository flags;
    private final NotificationService notifications;

    public AmlService(AmlFlagRepository flags, NotificationService notifications) {
        this.flags = flags;
        this.notifications = notifications;
    }

    @Transactional
    public void raise(UUID userId, UUID circleId, AmlFlag.Type type, AmlFlag.Severity severity, String reason) {
        if (flags.existsByUserIdAndCircleIdAndTypeAndStatus(userId, circleId, type, AmlFlag.Status.OPEN)) {
            return; // already an open case of this kind
        }
        AmlFlag flag = new AmlFlag();
        flag.setUserId(userId);
        flag.setCircleId(circleId);
        flag.setType(type);
        flag.setSeverity(severity);
        flag.setReason(reason);
        flags.save(flag);
        notifications.notify(userId, "AML_FLAG", type + " (" + severity + "): " + reason);
    }

    @Transactional(readOnly = true)
    public List<AmlFlag> openFlags() {
        return flags.findByStatusOrderByCreatedAtDesc(AmlFlag.Status.OPEN);
    }

    @Transactional
    public AmlFlag clear(UUID flagId) {
        AmlFlag flag = flags.findById(flagId).orElseThrow();
        flag.setStatus(AmlFlag.Status.CLEARED);
        return flags.save(flag);
    }
}
