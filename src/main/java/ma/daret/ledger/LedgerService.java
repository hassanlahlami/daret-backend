package ma.daret.ledger;

import jakarta.annotation.Nullable;
import ma.daret.common.web.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Posts balanced double-entry transactions. Every posting is validated to sum to
 * zero before any row is written; balances are always derived from entries.
 */
@Service
public class LedgerService {

    private final LedgerEntryRepository repository;

    public LedgerService(LedgerEntryRepository repository) {
        this.repository = repository;
    }

    /** A single leg: a debit or a credit on an account (the other side is zero). */
    public record Leg(String account, BigDecimal debit, BigDecimal credit) {
        public static Leg debit(String account, BigDecimal amount) {
            return new Leg(account, amount, BigDecimal.ZERO);
        }

        public static Leg credit(String account, BigDecimal amount) {
            return new Leg(account, BigDecimal.ZERO, amount);
        }
    }

    /**
     * Write a balanced posting. Must be called inside the same transaction as the
     * business state change it records. Throws if debits ≠ credits.
     */
    @Transactional
    public UUID post(String refType, UUID refId, @Nullable Integer cycleIndex, List<Leg> legs) {
        if (legs.size() < 2) {
            throw new IllegalArgumentException("A posting needs at least two legs");
        }
        BigDecimal debits = legs.stream().map(Leg::debit).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal credits = legs.stream().map(Leg::credit).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (debits.compareTo(credits) != 0) {
            throw ApiException.badRequest("LEDGER_UNBALANCED",
                    "Posting does not balance: debits=" + debits + " credits=" + credits);
        }

        UUID txId = UUID.randomUUID();
        List<LedgerEntry> entries = new ArrayList<>(legs.size());
        for (Leg leg : legs) {
            LedgerEntry e = new LedgerEntry();
            e.setTxId(txId);
            e.setAccount(leg.account());
            e.setDebit(leg.debit());
            e.setCredit(leg.credit());
            e.setRefType(refType);
            e.setRefId(refId);
            e.setCycleIndex(cycleIndex);
            entries.add(e);
        }
        repository.saveAll(entries);
        return txId;
    }

    @Transactional(readOnly = true)
    public BigDecimal balanceOf(String account) {
        BigDecimal b = repository.balanceOf(account);
        return b != null ? b : BigDecimal.ZERO;
    }
}
