package ma.daret.circles;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface CircleRepository extends JpaRepository<Circle, UUID>, JpaSpecificationExecutor<Circle> {

    long countByStatus(CircleStatus status);

    Page<Circle> findByStatus(CircleStatus status, Pageable pageable);

    java.util.List<Circle> findByCreatedAtAfter(java.time.Instant after);

    /**
     * Locks the circle row for the duration of the transaction so concurrent joins
     * are serialized and cannot oversubscribe seats.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Circle c where c.id = :id")
    Optional<Circle> findByIdForUpdate(@Param("id") UUID id);
}
