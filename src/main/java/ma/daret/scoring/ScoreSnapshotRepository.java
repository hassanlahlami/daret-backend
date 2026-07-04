package ma.daret.scoring;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ScoreSnapshotRepository extends JpaRepository<ScoreSnapshot, UUID> {

    List<ScoreSnapshot> findByUserIdOrderByCreatedAtDesc(UUID userId, Limit limit);
}
