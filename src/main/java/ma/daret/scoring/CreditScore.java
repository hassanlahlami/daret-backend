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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * A user's credit score (0–1000) and tier A–E (spec FR-5, §8). {@code factors}
 * stores the per-component contribution to the score for explainability — a
 * requirement for the CNDP profiling basis.
 */
@Entity
@Table(name = "credit_scores")
@Getter
@Setter
@NoArgsConstructor
public class CreditScore extends BaseEntity {

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(nullable = false)
    private int score;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CreditTier tier;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, BigDecimal> factors = Map.of();

    @Column(name = "computed_at", nullable = false)
    private Instant computedAt = Instant.now();

    public CreditScore(UUID userId) {
        this.userId = userId;
    }
}
