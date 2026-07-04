package ma.daret.scoring;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ma.daret.common.domain.BaseEntity;

import java.util.UUID;

/** A point-in-time credit-score record, written whenever the score changes. */
@Entity
@Table(name = "score_snapshots")
@Getter
@Setter
@NoArgsConstructor
public class ScoreSnapshot extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private int score;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CreditTier tier;

    public ScoreSnapshot(UUID userId, int score, CreditTier tier) {
        this.userId = userId;
        this.score = score;
        this.tier = tier;
    }
}
