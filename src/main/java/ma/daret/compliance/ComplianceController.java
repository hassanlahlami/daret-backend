package ma.daret.compliance;

import ma.daret.identity.kyc.KycProfile;
import ma.daret.identity.kyc.KycService;
import ma.daret.identity.kyc.KycStatus;
import org.springframework.data.domain.Limit;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Read-mostly compliance console (spec §6, §13, FR-18/19). Restricted to
 * COMPLIANCE_OFFICER: the audit chain + integrity verification, the KYC review
 * queue and decisions, and AML flags.
 */
@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('COMPLIANCE_OFFICER')")
public class ComplianceController {

    private final AuditService auditService;
    private final AuditEventRepository auditEvents;
    private final KycService kycService;
    private final AmlService amlService;

    public ComplianceController(AuditService auditService,
                                AuditEventRepository auditEvents,
                                KycService kycService,
                                AmlService amlService) {
        this.auditService = auditService;
        this.auditEvents = auditEvents;
        this.kycService = kycService;
        this.amlService = amlService;
    }

    // ---- Audit chain ----

    public record AuditEventView(long seq, String actor, String action, String target,
                                 String payload, String prevHash, String hash, Instant at) {
        static AuditEventView from(AuditEvent e) {
            return new AuditEventView(e.getSeq(), e.getActor(), e.getAction(), e.getTarget(),
                    e.getPayload(), e.getPrevHash(), e.getHash(), e.getCreatedAt());
        }
    }

    @GetMapping("/audit")
    public List<AuditEventView> chain(@RequestParam(defaultValue = "100") int limit) {
        return auditEvents.findByOrderBySeqDesc(Limit.of(limit)).stream()
                .map(AuditEventView::from)
                .toList();
    }

    @GetMapping("/audit/verify")
    public AuditService.IntegrityReport verify() {
        return auditService.verify();
    }

    // ---- KYC review (FR-19) ----

    public record KycQueueItem(UUID userId, KycStatus status) {
    }

    public record KycDecisionRequest(KycStatus decision) {
    }

    @GetMapping("/compliance/kyc")
    public List<KycQueueItem> kycQueue() {
        return kycService.pendingQueue().stream()
                .map(p -> new KycQueueItem(p.getUserId(), p.getStatus()))
                .toList();
    }

    @PostMapping("/compliance/kyc/{userId}/decision")
    public KycQueueItem decideKyc(@PathVariable UUID userId, @RequestBody KycDecisionRequest req) {
        KycProfile p = kycService.decide(userId, req.decision());
        return new KycQueueItem(p.getUserId(), p.getStatus());
    }

    // ---- AML flags (FR-19) ----

    public record AmlFlagView(UUID id, UUID userId, UUID circleId, String type,
                              String severity, String reason) {
        static AmlFlagView from(AmlFlag f) {
            return new AmlFlagView(f.getId(), f.getUserId(), f.getCircleId(),
                    f.getType().name(), f.getSeverity().name(), f.getReason());
        }
    }

    @GetMapping("/compliance/aml-flags")
    public List<AmlFlagView> amlFlags() {
        return amlService.openFlags().stream().map(AmlFlagView::from).toList();
    }

    @PostMapping("/compliance/aml-flags/{id}/clear")
    public AmlFlagView clearAmlFlag(@PathVariable UUID id) {
        return AmlFlagView.from(amlService.clear(id));
    }
}
