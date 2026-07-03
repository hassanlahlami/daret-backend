package ma.daret.payments;

import ma.daret.risk.DefaultSweepService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * Operational trigger for the money pipeline (spec §10.6 jobs also run on a timer).
 * ADMIN-only. {@code asOf} lets operations replay collections up to a date — also
 * how the full lifecycle is driven in demos without waiting real cycle cadences.
 */
@RestController
@RequestMapping("/api/v1/admin/ops")
@PreAuthorize("hasRole('ADMIN')")
public class OpsController {

    private final CyclePipeline pipeline;
    private final DefaultSweepService defaultSweep;
    private final PaymentReconciliationService reconciliation;

    public OpsController(CyclePipeline pipeline, DefaultSweepService defaultSweep,
                         PaymentReconciliationService reconciliation) {
        this.pipeline = pipeline;
        this.defaultSweep = defaultSweep;
        this.reconciliation = reconciliation;
    }

    /** Reconcile still-pending payment intents against the PSP (poll path). */
    @PostMapping("/reconcile")
    public java.util.Map<String, Integer> reconcile() {
        return java.util.Map.of("settled", reconciliation.reconcilePending());
    }

    @PostMapping("/run-cycle")
    public CyclePipeline.Result runCycle(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOf) {
        return pipeline.run(asOf != null ? asOf : LocalDate.now());
    }

    @PostMapping("/run-default-sweep")
    public DefaultSweepService.SweepResult runDefaultSweep(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOf) {
        return defaultSweep.sweep(asOf != null ? asOf : LocalDate.now());
    }
}
