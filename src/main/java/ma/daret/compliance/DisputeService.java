package ma.daret.compliance;

import ma.daret.common.web.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class DisputeService {

    private final DisputeRepository disputes;

    public DisputeService(DisputeRepository disputes) {
        this.disputes = disputes;
    }

    @Audited("dispute.raise")
    @Transactional
    public Dispute raise(UUID raiserUserId, UUID circleId, String subject, String description) {
        if (subject == null || subject.isBlank() || description == null || description.isBlank()) {
            throw ApiException.badRequest("DISPUTE_INCOMPLETE", "Subject and description are required");
        }
        return disputes.save(new Dispute(raiserUserId, circleId, subject.trim(), description.trim()));
    }

    @Transactional(readOnly = true)
    public List<Dispute> mine(UUID raiserUserId) {
        return disputes.findByRaiserUserIdOrderByCreatedAtDesc(raiserUserId);
    }

    @Transactional(readOnly = true)
    public List<Dispute> open() {
        return disputes.findByStatusOrderByCreatedAtDesc(Dispute.Status.OPEN);
    }

    @Audited("dispute.resolve")
    @Transactional
    public Dispute resolve(UUID disputeId, String resolution) {
        Dispute d = disputes.findById(disputeId)
                .orElseThrow(() -> ApiException.notFound("DISPUTE_NOT_FOUND", "Dispute not found"));
        d.setStatus(Dispute.Status.RESOLVED);
        d.setResolution(resolution);
        return disputes.save(d);
    }
}
