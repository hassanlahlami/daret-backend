package ma.daret.identity.kyc;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ma.daret.common.domain.BaseEntity;

import java.util.UUID;

/**
 * A user's KYC state. Raw identity documents are NEVER stored here — only an
 * opaque reference ({@code partnerRef}) returned by the KYC/payment partner.
 */
@Entity
@Table(name = "kyc_profiles")
@Getter
@Setter
@NoArgsConstructor
public class KycProfile extends BaseEntity {

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private KycStatus status = KycStatus.PENDING;

    @Column(name = "partner_ref")
    private String partnerRef;

    public KycProfile(UUID userId) {
        this.userId = userId;
        this.status = KycStatus.PENDING;
    }
}
