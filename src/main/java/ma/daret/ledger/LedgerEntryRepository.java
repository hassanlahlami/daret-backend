package ma.daret.ledger;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.UUID;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, UUID> {

    /** Derived balance for an account: total credits minus total debits. */
    @Query("""
            select coalesce(sum(e.credit), 0) - coalesce(sum(e.debit), 0)
            from LedgerEntry e where e.account = :account
            """)
    BigDecimal balanceOf(@Param("account") String account);

    /** Global zero-sum check across the whole ledger (should always be zero). */
    @Query("select coalesce(sum(e.credit), 0) - coalesce(sum(e.debit), 0) from LedgerEntry e")
    BigDecimal globalBalance();
}
