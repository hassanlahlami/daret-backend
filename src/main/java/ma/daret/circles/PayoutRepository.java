package ma.daret.circles;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PayoutRepository extends JpaRepository<Payout, UUID> {

    List<Payout> findByCircleId(UUID circleId);

    List<Payout> findByCircleIdAndStatus(UUID circleId, Payout.Status status);

    Optional<Payout> findByCircleIdAndCycleIndex(UUID circleId, int cycleIndex);

    List<Payout> findByRecipientUserId(UUID recipientUserId);
}
