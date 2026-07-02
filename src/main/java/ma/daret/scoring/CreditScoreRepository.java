package ma.daret.scoring;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CreditScoreRepository extends JpaRepository<CreditScore, UUID> {

    Optional<CreditScore> findByUserId(UUID userId);
}
