package ma.daret.compliance;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ErasureRequestRepository extends JpaRepository<ErasureRequest, UUID> {

    List<ErasureRequest> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<ErasureRequest> findByStatusOrderByCreatedAtDesc(ErasureRequest.Status status);
}
