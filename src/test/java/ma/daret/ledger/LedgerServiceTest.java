package ma.daret.ledger;

import ma.daret.common.web.ApiException;
import ma.daret.ledger.LedgerService.Leg;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LedgerServiceTest {

    private final LedgerEntryRepository repository = mock(LedgerEntryRepository.class);
    private final LedgerService service = new LedgerService(repository);

    @Test
    void balanced_posting_is_saved_with_a_shared_tx_id() {
        UUID ref = UUID.randomUUID();
        service.post("CONTRIBUTION", ref, 1, List.of(
                Leg.debit("member:x", new BigDecimal("100.00")),
                Leg.credit("circle:y:pot", new BigDecimal("100.00"))
        ));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<LedgerEntry>> captor = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(captor.capture());
        List<LedgerEntry> saved = captor.getValue();
        assertThat(saved).hasSize(2);
        assertThat(saved.get(0).getTxId()).isEqualTo(saved.get(1).getTxId());
    }

    @Test
    void unbalanced_posting_is_rejected_and_nothing_is_written() {
        assertThatThrownBy(() -> service.post("PAYOUT", UUID.randomUUID(), 1, List.of(
                Leg.debit("circle:y:pot", new BigDecimal("100.00")),
                Leg.credit("member:x", new BigDecimal("90.00"))
        )))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("does not balance");

        verify(repository, never()).saveAll(org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    void balance_is_derived_from_entries() {
        when(repository.balanceOf("platform:revenue")).thenReturn(new BigDecimal("125.00"));
        assertThat(service.balanceOf("platform:revenue")).isEqualByComparingTo("125.00");
    }
}
