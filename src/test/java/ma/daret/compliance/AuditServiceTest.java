package ma.daret.compliance;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuditServiceTest {

    private final AuditEventRepository repository = mock(AuditEventRepository.class);
    private final AuditService service = new AuditService(repository);

    private AuditEvent linked(long seq, String prev, String payload) {
        AuditEvent e = new AuditEvent();
        e.setSeq(seq);
        e.setActor("system");
        e.setAction("test.action");
        e.setTarget(null);
        e.setPayload(payload);
        e.setPrevHash(prev);
        e.setHash(AuditService.hash(prev, seq, "system", "test.action", null, payload));
        return e;
    }

    @Test
    void intact_chain_verifies() {
        AuditEvent e1 = linked(1, AuditService.GENESIS, "{\"a\":1}");
        AuditEvent e2 = linked(2, e1.getHash(), "{\"a\":2}");
        when(repository.findAllByOrderBySeqAsc()).thenReturn(List.of(e1, e2));

        AuditService.IntegrityReport report = service.verify();
        assertThat(report.valid()).isTrue();
        assertThat(report.eventsChecked()).isEqualTo(2);
        assertThat(report.brokenAtSeq()).isNull();
    }

    @Test
    void tampered_payload_breaks_the_chain() {
        AuditEvent e1 = linked(1, AuditService.GENESIS, "{\"a\":1}");
        AuditEvent e2 = linked(2, e1.getHash(), "{\"a\":2}");
        e2.setPayload("{\"a\":999}"); // tamper without recomputing the hash
        when(repository.findAllByOrderBySeqAsc()).thenReturn(List.of(e1, e2));

        AuditService.IntegrityReport report = service.verify();
        assertThat(report.valid()).isFalse();
        assertThat(report.brokenAtSeq()).isEqualTo(2);
    }
}
