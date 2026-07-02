package ma.daret.risk;

import ma.daret.common.web.ApiException;
import org.springframework.stereotype.Service;

/**
 * Enforces the capital-light gap-fill rule (spec §5.4, FR-16). The platform may
 * fund empty seats in an under-filled circle only up to {@code maxPlatformExposurePct}
 * of the seats; otherwise activation is rejected.
 */
@Service
public class RiskService {

    private final RiskProperties props;

    public RiskService(RiskProperties props) {
        this.props = props;
    }

    /** How many empty seats the platform will fund, or a rejection if over the cap. */
    public record GapFillPlan(int emptySeats, double exposurePct) {
    }

    public GapFillPlan planGapFill(int seatCount, int filledSeats) {
        int empty = seatCount - filledSeats;
        if (empty < 0) {
            throw ApiException.conflict("OVERFILLED", "Circle has more members than seats");
        }
        if (empty == 0) {
            return new GapFillPlan(0, 0.0);
        }
        double exposure = (double) empty / seatCount;
        if (exposure > props.maxPlatformExposurePct()) {
            throw ApiException.conflict("EXPOSURE_EXCEEDED",
                    "Gap-filling " + empty + "/" + seatCount + " seats ("
                            + Math.round(exposure * 100) + "%) exceeds the "
                            + Math.round(props.maxPlatformExposurePct() * 100) + "% platform exposure cap");
        }
        return new GapFillPlan(empty, exposure);
    }
}
