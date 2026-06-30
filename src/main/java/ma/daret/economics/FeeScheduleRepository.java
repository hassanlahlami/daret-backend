package ma.daret.economics;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface FeeScheduleRepository extends JpaRepository<FeeSchedule, UUID> {

    /** The fixed-id default schedule used when a circle does not specify one. */
    UUID DEFAULT_ID = UUID.fromString("00000000-0000-0000-0000-0000000000d1");

    Optional<FeeSchedule> findByName(String name);
}
