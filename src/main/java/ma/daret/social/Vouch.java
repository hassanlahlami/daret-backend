package ma.daret.social;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ma.daret.common.domain.BaseEntity;

import java.util.UUID;

/**
 * A peer trust signal: {@code voucher} vouches for {@code vouchee}. Feeds the
 * credit score (spec §19 — the local trust differentiator).
 */
@Entity
@Table(name = "vouches", uniqueConstraints =
        @UniqueConstraint(name = "uq_vouch", columnNames = {"voucher_user_id", "vouchee_user_id"}))
@Getter
@Setter
@NoArgsConstructor
public class Vouch extends BaseEntity {

    @Column(name = "voucher_user_id", nullable = false)
    private UUID voucherUserId;

    @Column(name = "vouchee_user_id", nullable = false)
    private UUID voucheeUserId;

    public Vouch(UUID voucherUserId, UUID voucheeUserId) {
        this.voucherUserId = voucherUserId;
        this.voucheeUserId = voucheeUserId;
    }
}
