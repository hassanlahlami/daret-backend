package ma.daret.circles;

import ma.daret.common.web.ApiException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CircleStateMachineTest {

    private final CircleStateMachine sm = new CircleStateMachine();

    @Test
    void allows_documented_transitions() {
        assertThat(sm.canTransition(CircleStatus.DRAFT, CircleStatus.FORMING)).isTrue();
        assertThat(sm.canTransition(CircleStatus.FORMING, CircleStatus.ACTIVE)).isTrue();
        assertThat(sm.canTransition(CircleStatus.FORMING, CircleStatus.DISSOLVED)).isTrue();
        assertThat(sm.canTransition(CircleStatus.ACTIVE, CircleStatus.COMPLETED)).isTrue();
        assertThat(sm.canTransition(CircleStatus.ACTIVE, CircleStatus.DISSOLVED)).isTrue();
    }

    @Test
    void rejects_illegal_transitions() {
        assertThat(sm.canTransition(CircleStatus.FORMING, CircleStatus.COMPLETED)).isFalse();
        assertThat(sm.canTransition(CircleStatus.COMPLETED, CircleStatus.ACTIVE)).isFalse();
        assertThat(sm.canTransition(CircleStatus.DISSOLVED, CircleStatus.FORMING)).isFalse();
    }

    @Test
    void transition_mutates_on_legal_and_throws_on_illegal() {
        Circle c = new Circle();
        c.setStatus(CircleStatus.FORMING);

        sm.transition(c, CircleStatus.ACTIVE);
        assertThat(c.getStatus()).isEqualTo(CircleStatus.ACTIVE);

        assertThatThrownBy(() -> sm.transition(c, CircleStatus.FORMING))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Cannot move circle");
    }
}
