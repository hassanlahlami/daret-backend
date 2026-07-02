package ma.daret.risk;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Risk knobs (spec §5.4). {@code maxPlatformExposurePct} caps the fraction of a
 * circle's seats the platform may gap-fill. {@code contributionGraceDays} is the
 * window before an unpaid contribution is marked MISSED; a member is DEFAULTED
 * once they accumulate {@code defaultAfterMissed} missed contributions.
 */
@ConfigurationProperties(prefix = "daret.risk")
public record RiskProperties(double maxPlatformExposurePct, int contributionGraceDays, int defaultAfterMissed) {

    public RiskProperties {
        if (maxPlatformExposurePct < 0 || maxPlatformExposurePct > 1) {
            throw new IllegalArgumentException("maxPlatformExposurePct must be between 0 and 1");
        }
    }
}
