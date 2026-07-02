package ma.daret.common.pii;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PiiMaskingModuleTest {

    static class Person {
        public String name = "Amina";
        @Pii
        public String email = "amina@example.ma";
        @Pii
        public String phone = "+212600000000";
    }

    @Test
    void masks_pii_fields_but_keeps_the_rest() throws Exception {
        ObjectMapper mapper = new ObjectMapper().registerModule(new PiiMaskingModule());
        String json = mapper.writeValueAsString(new Person());

        assertThat(json).contains("\"name\":\"Amina\"");
        assertThat(json).contains("\"email\":\"***\"");
        assertThat(json).contains("\"phone\":\"***\"");
        assertThat(json).doesNotContain("amina@example.ma");
        assertThat(json).doesNotContain("+212600000000");
    }
}
