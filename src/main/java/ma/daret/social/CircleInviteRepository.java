package ma.daret.social;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CircleInviteRepository extends JpaRepository<CircleInvite, UUID> {

    Optional<CircleInvite> findByCode(String code);
}
