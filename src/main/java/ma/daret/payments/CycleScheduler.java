package ma.daret.payments;

import ma.daret.risk.DefaultSweepService;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Runs the money pipeline on a timer. Guarded by ShedLock so only one instance
 * fires at a time. Disable with {@code daret.scheduler.enabled=false} (used in
 * tests, which drive the pipeline explicitly).
 */
@Component
@ConditionalOnProperty(value = "daret.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class CycleScheduler {

    private static final Logger log = LoggerFactory.getLogger(CycleScheduler.class);

    private final CyclePipeline pipeline;
    private final DefaultSweepService defaultSweep;

    public CycleScheduler(CyclePipeline pipeline, DefaultSweepService defaultSweep) {
        this.pipeline = pipeline;
        this.defaultSweep = defaultSweep;
    }

    @Scheduled(fixedDelayString = "${daret.scheduler.interval-ms:60000}")
    @SchedulerLock(name = "cyclePipeline", lockAtLeastFor = "PT5S", lockAtMostFor = "PT5M")
    public void runCycle() {
        CyclePipeline.Result result = pipeline.runToday();
        if (result.collectionsIssued() > 0 || result.payoutsIssued() > 0) {
            log.info("Cycle pipeline: {} collections, {} payouts, {} events relayed",
                    result.collectionsIssued(), result.payoutsIssued(), result.eventsRelayed());
        }
    }

    @Scheduled(fixedDelayString = "${daret.scheduler.interval-ms:60000}")
    @SchedulerLock(name = "defaultSweep", lockAtLeastFor = "PT5S", lockAtMostFor = "PT5M")
    public void runDefaultSweep() {
        DefaultSweepService.SweepResult result = defaultSweep.sweep(LocalDate.now());
        if (result.missedMarked() > 0) {
            log.info("Default sweep: {} missed, {} late, {} defaulted",
                    result.missedMarked(), result.membersLate(), result.membersDefaulted());
        }
    }
}
