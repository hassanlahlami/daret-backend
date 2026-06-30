package ma.daret.circles;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CircleMemberRepository extends JpaRepository<CircleMember, UUID> {

    List<CircleMember> findByCircleId(UUID circleId);

    List<CircleMember> findByUserId(UUID userId);

    int countByCircleId(UUID circleId);

    boolean existsByCircleIdAndUserId(UUID circleId, UUID userId);
}
