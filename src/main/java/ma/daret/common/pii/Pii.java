package ma.daret.common.pii;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a field as personal data (CNDP / Loi 09-08, spec §12). Marked fields are
 * masked by default in audit payloads (and can be masked in logs), so PII never
 * lands in the audit chain or log files.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.RECORD_COMPONENT})
public @interface Pii {
}
