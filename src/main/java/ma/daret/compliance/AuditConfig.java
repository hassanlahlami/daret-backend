package ma.daret.compliance;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import ma.daret.common.pii.PiiMaskingModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * A dedicated {@code ObjectMapper} for audit payloads: masks {@link ma.daret.common.pii.Pii}
 * fields and tolerates odd shapes so serialization never breaks an audited call.
 */
@Configuration
public class AuditConfig {

    @Bean(name = "auditObjectMapper")
    public ObjectMapper auditObjectMapper() {
        return new ObjectMapper()
                .registerModule(new PiiMaskingModule())
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }
}
