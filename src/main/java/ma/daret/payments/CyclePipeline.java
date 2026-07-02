package ma.daret.payments;

import ma.daret.payments.outbox.OutboxRelay;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/**
 * Orchestrates one pass of the money cycle: issue due collections → relay →
 * issue any now-payable payouts → relay, iterating until no further payouts can
 * be issued (later cycles unlock as earlier contributions settle). Idempotent, so
 * it is safe to run on a timer and on demand.
 */
@Service
public class CyclePipeline {

    private static final int MAX_ITERATIONS = 64;

    private final CollectionService collectionService;
    private final PayoutService payoutService;
    private final OutboxRelay relay;

    public CyclePipeline(CollectionService collectionService,
                         PayoutService payoutService,
                         OutboxRelay relay) {
        this.collectionService = collectionService;
        this.payoutService = payoutService;
        this.relay = relay;
    }

    public record Result(int collectionsIssued, int payoutsIssued, int eventsRelayed) {
    }

    /** Run the pipeline as of the given date (contributions due on/before it). */
    public Result run(LocalDate asOf) {
        int collections = collectionService.issueDueCollections(asOf);
        int relayed = relay.relayPending();

        int payoutsTotal = 0;
        for (int i = 0; i < MAX_ITERATIONS; i++) {
            int payouts = payoutService.issueDuePayouts();
            payoutsTotal += payouts;
            relayed += relay.relayPending();
            if (payouts == 0) {
                break;
            }
        }
        return new Result(collections, payoutsTotal, relayed);
    }

    /** Convenience for the scheduler: run as of today. */
    public Result runToday() {
        return run(LocalDate.now());
    }
}
