package ma.daret.payments.webhook;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WebhookSignatureTest {

    private static final String SECRET = "test-secret";

    @Test
    void a_valid_signature_verifies() {
        String body = "{\"externalRef\":\"mock_1\",\"status\":\"SETTLED\"}";
        String sig = WebhookSignature.sign(SECRET, body);
        assertThat(WebhookSignature.verify(SECRET, body, sig)).isTrue();
    }

    @Test
    void a_tampered_body_or_wrong_secret_or_missing_sig_fails() {
        String body = "{\"externalRef\":\"mock_1\",\"status\":\"SETTLED\"}";
        String sig = WebhookSignature.sign(SECRET, body);

        assertThat(WebhookSignature.verify(SECRET, body + " ", sig)).isFalse();
        assertThat(WebhookSignature.verify("other-secret", body, sig)).isFalse();
        assertThat(WebhookSignature.verify(SECRET, body, null)).isFalse();
    }
}
