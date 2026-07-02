package ma.daret.identity.kyc;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface KycProfileRepository extends JpaRepository<KycProfile, UUID> {

    Optional<KycProfile> findByUserId(UUID userId);

    List<KycProfile> findByStatus(KycStatus status);
}
