package ma.daret.compliance;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AmlFlagRepository extends JpaRepository<AmlFlag, UUID> {

    List<AmlFlag> findByStatusOrderByCreatedAtDesc(AmlFlag.Status status);

    boolean existsByUserIdAndCircleIdAndTypeAndStatus(
            UUID userId, UUID circleId, AmlFlag.Type type, AmlFlag.Status status);
}
