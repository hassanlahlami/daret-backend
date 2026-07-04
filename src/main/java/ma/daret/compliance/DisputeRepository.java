package ma.daret.compliance;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DisputeRepository extends JpaRepository<Dispute, UUID> {

    List<Dispute> findByStatusOrderByCreatedAtDesc(Dispute.Status status);

    List<Dispute> findByRaiserUserIdOrderByCreatedAtDesc(UUID raiserUserId);

    long countByStatus(Dispute.Status status);
}
