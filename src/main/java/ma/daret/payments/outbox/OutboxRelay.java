package ma.daret.payments.outbox;

import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Component;

/**
 * Drains the transactional outbox (spec §10.5). Reads PENDING events and hands each
 * to {@link OutboxProcessor} in its own transaction, so one poisoned event cannot
 * roll back the rest of the batch.
 */
@Component
public class OutboxRelay {

    private static final int BATCH = 100;

    private final OutboxEventRepository outbox;
    private final OutboxProcessor processor;

    public OutboxRelay(OutboxEventRepository outbox, OutboxProcessor processor) {
        this.outbox = outbox;
        this.processor = processor;
    }

    /** @return number of events processed. */
    public int relayPending() {
        var pending = outbox.findByStatusOrderByCreatedAtAsc(OutboxEvent.Status.PENDING, Limit.of(BATCH));
        int processed = 0;
        for (OutboxEvent event : pending) {
            processor.process(event.getId(), outbox);
            processed++;
        }
        return processed;
    }
}
