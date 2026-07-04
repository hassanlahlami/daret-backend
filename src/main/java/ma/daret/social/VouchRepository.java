package ma.daret.social;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface VouchRepository extends JpaRepository<Vouch, UUID> {

    long countByVoucheeUserId(UUID voucheeUserId);

    boolean existsByVoucherUserIdAndVoucheeUserId(UUID voucherUserId, UUID voucheeUserId);
}
