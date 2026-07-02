package ma.daret.compliance;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;

/**
 * Appends to and verifies the tamper-evident audit chain (spec §13). Appends are
 * serialized so the chain stays strictly ordered and each hash links the previous.
 */
@Service
public class AuditService {

    /** prevHash of the very first event. */
    static final String GENESIS = "0".repeat(64);

    private final AuditEventRepository repository;

    public AuditService(AuditEventRepository repository) {
        this.repository = repository;
    }

    /**
     * Append one event. {@code synchronized} keeps the chain consistent on a single
     * instance (a shared lock would be needed to scale writes horizontally).
     */
    @Transactional
    public synchronized AuditEvent append(String actor, String action, String target, String payload) {
        AuditEvent last = repository.findFirstByOrderBySeqDesc().orElse(null);
        long seq = last == null ? 1 : last.getSeq() + 1;
        String prevHash = last == null ? GENESIS : last.getHash();

        AuditEvent event = new AuditEvent();
        event.setSeq(seq);
        event.setActor(actor);
        event.setAction(action);
        event.setTarget(target);
        event.setPayload(payload);
        event.setPrevHash(prevHash);
        event.setHash(hash(prevHash, seq, actor, action, target, payload));
        return repository.save(event);
    }

    public record IntegrityReport(boolean valid, long eventsChecked, Long brokenAtSeq) {
    }

    /** Recompute the whole chain and flag the first break, if any (FR-18). */
    @Transactional(readOnly = true)
    public IntegrityReport verify() {
        List<AuditEvent> events = repository.findAllByOrderBySeqAsc();
        String prev = GENESIS;
        long checked = 0;
        for (AuditEvent e : events) {
            checked++;
            String expected = hash(prev, e.getSeq(), e.getActor(), e.getAction(), e.getTarget(), e.getPayload());
            if (!prev.equals(e.getPrevHash()) || !expected.equals(e.getHash())) {
                return new IntegrityReport(false, checked, e.getSeq());
            }
            prev = e.getHash();
        }
        return new IntegrityReport(true, checked, null);
    }

    static String hash(String prevHash, long seq, String actor, String action, String target, String payload) {
        String canonical = String.join("|",
                prevHash, Long.toString(seq), actor, action, target == null ? "" : target, payload);
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
