package ma.daret.circles;

import ma.daret.common.web.ApiException;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * The single authority on legal circle transitions (spec §9.1). Any attempt to
 * move a circle along an undefined edge raises a domain exception rather than
 * silently mutating state.
 */
@Component
public class CircleStateMachine {

    private static final Map<CircleStatus, Set<CircleStatus>> ALLOWED = new EnumMap<>(CircleStatus.class);

    static {
        ALLOWED.put(CircleStatus.DRAFT, EnumSet.of(CircleStatus.FORMING, CircleStatus.DISSOLVED));
        ALLOWED.put(CircleStatus.FORMING, EnumSet.of(CircleStatus.ACTIVE, CircleStatus.DISSOLVED));
        ALLOWED.put(CircleStatus.ACTIVE, EnumSet.of(CircleStatus.COMPLETED, CircleStatus.DISSOLVED));
        ALLOWED.put(CircleStatus.COMPLETED, EnumSet.noneOf(CircleStatus.class));
        ALLOWED.put(CircleStatus.DISSOLVED, EnumSet.noneOf(CircleStatus.class));
    }

    public boolean canTransition(CircleStatus from, CircleStatus to) {
        return ALLOWED.getOrDefault(from, EnumSet.noneOf(CircleStatus.class)).contains(to);
    }

    /**
     * Apply a transition to the circle, or throw if it is not permitted from the
     * circle's current status.
     */
    public void transition(Circle circle, CircleStatus to) {
        CircleStatus from = circle.getStatus();
        if (!canTransition(from, to)) {
            throw ApiException.conflict("ILLEGAL_TRANSITION",
                    "Cannot move circle from " + from + " to " + to);
        }
        circle.setStatus(to);
    }
}
