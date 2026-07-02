package ma.daret.compliance;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a service method whose successful invocation must emit an {@link AuditEvent}
 * (spec §13). The {@link AuditAspect} captures the actor, this action name, a
 * best-effort target id, and a PII-masked payload of the arguments.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Audited {

    /** Stable action name, e.g. {@code "circle.create"}. */
    String value();
}
