package ma.daret.support;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.DockerClientFactory;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Disables (skips, not fails) a Testcontainers-backed test when no Docker daemon is
 * reachable — e.g. local machines where Docker Desktop's named pipe is unavailable.
 * The condition is evaluated before the Testcontainers extension starts containers.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@ExtendWith(EnabledIfDockerAvailable.DockerCondition.class)
public @interface EnabledIfDockerAvailable {

    class DockerCondition implements ExecutionCondition {
        @Override
        public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
            try {
                if (DockerClientFactory.instance().isDockerAvailable()) {
                    return ConditionEvaluationResult.enabled("Docker is available");
                }
            } catch (Throwable ignored) {
                // fall through to disabled
            }
            return ConditionEvaluationResult.disabled("Docker not available — skipping integration test");
        }
    }
}
