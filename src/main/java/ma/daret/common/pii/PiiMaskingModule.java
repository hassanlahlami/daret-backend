package ma.daret.common.pii;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;

import java.io.IOException;
import java.util.List;

/**
 * Jackson module that replaces any {@link Pii}-annotated property's value with
 * {@code "***"} during serialization. Registered on the dedicated audit
 * {@code ObjectMapper} so audit payloads are PII-free by construction.
 */
public class PiiMaskingModule extends SimpleModule {

    private static final String MASK = "***";

    public PiiMaskingModule() {
        setSerializerModifier(new BeanSerializerModifier() {
            @Override
            public List<BeanPropertyWriter> changeProperties(SerializationConfig config,
                                                             BeanDescription beanDesc,
                                                             List<BeanPropertyWriter> beanProperties) {
                for (BeanPropertyWriter writer : beanProperties) {
                    if (writer.getMember() != null && writer.getMember().hasAnnotation(Pii.class)) {
                        writer.assignSerializer(new MaskingSerializer());
                    }
                }
                return beanProperties;
            }
        });
    }

    private static final class MaskingSerializer extends JsonSerializer<Object> {
        @Override
        public void serialize(Object value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeString(MASK);
        }
    }
}
