package ma.daret.waitlist;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WaitlistRepository extends JpaRepository<WaitlistEntry, UUID> {

    Optional<WaitlistEntry> findByEmail(String email);

    List<WaitlistEntry> findAllByOrderByCreatedAtDesc();
}
