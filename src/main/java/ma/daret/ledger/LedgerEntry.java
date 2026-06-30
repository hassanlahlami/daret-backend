package ma.daret.ledger;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ma.daret.common.domain.BaseEntity;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * One leg of a double-entry posting (spec §10.3). Append-only — rows are never
 * updated or deleted. Each entry carries exactly one of {@code debit}/{@code credit}
 * (the other is zero); the entries of a single {@code txId} always sum to zero.
 *
 * <p>Accounts model claims/positions, never platform-owned spendable cash.
 * Balances are derived ({@code SUM(credit) - SUM(debit)}), never stored.
 */
@Entity
@Table(name = "ledger_entries")
@Getter
@Setter
@NoArgsConstructor
public class LedgerEntry extends BaseEntity {

    /** Groups the legs of one balanced posting. */
    @Column(name = "tx_id", nullable = false)
    private UUID txId;

    @Column(nullable = false)
    private String account;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal debit = BigDecimal.ZERO;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal credit = BigDecimal.ZERO;

    /** What this posting references, e.g. CONTRIBUTION / PAYOUT. */
    @Column(name = "ref_type", nullable = false)
    private String refType;

    @Column(name = "ref_id", nullable = false)
    private UUID refId;

    @Column(name = "cycle_index")
    private Integer cycleIndex;
}
