package ma.daret.circles;

/**
 * Circle lifecycle (spec §9.1):
 * {@code DRAFT → FORMING → ACTIVE → COMPLETED}, with {@code DISSOLVED} as the
 * terminal failure state from FORMING (unfillable/cancelled) or ACTIVE
 * (irrecoverable default). Transitions are enforced by {@link CircleStateMachine}.
 */
public enum CircleStatus {
    DRAFT,
    FORMING,
    ACTIVE,
    COMPLETED,
    DISSOLVED
}
