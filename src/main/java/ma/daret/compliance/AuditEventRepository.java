package ma.daret.compliance;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuditEventRepository extends JpaRepository<AuditEvent, UUID> {

    Optional<AuditEvent> findFirstByOrderBySeqDesc();

    List<AuditEvent> findAllByOrderBySeqAsc();

    List<AuditEvent> findByOrderBySeqDesc(Limit limit);
}
