package ma.daret.circles;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ContributionRepository extends JpaRepository<Contribution, UUID> {

    List<Contribution> findByCircleId(UUID circleId);

    List<Contribution> findByCircleIdAndCycleIndex(UUID circleId, int cycleIndex);

    List<Contribution> findByUserIdAndStatus(UUID userId, Contribution.Status status);

    /** Due, not-yet-settled obligations to collect on this run. */
    List<Contribution> findByStatusAndDueDateLessThanEqual(Contribution.Status status, LocalDate onOrBefore);

    int countByCircleIdAndCycleIndexAndStatusNot(UUID circleId, int cycleIndex, Contribution.Status status);
}
