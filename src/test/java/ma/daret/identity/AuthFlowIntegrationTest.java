package ma.daret.identity;

import com.fasterxml.jackson.databind.JsonNode;
import ma.daret.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.boot.test.web.client.TestRestTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises the Phase-1 identity slice end to end: register → authenticated KYC
 * call → refresh rotation, plus the auth gate on protected endpoints.
 */
class AuthFlowIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    TestRestTemplate rest;

    @Test
    void register_then_use_token_then_refresh() {
        // Register
        ResponseEntity<JsonNode> reg = rest.postForEntity("/api/v1/auth/register",
                Map.of("email", "amina@example.ma", "password", "supersecret1"), JsonNode.class);
        assertThat(reg.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String access = reg.getBody().get("accessToken").asText();
        String refresh = reg.getBody().get("refreshToken").asText();
        assertThat(access).isNotBlank();

        // Authenticated KYC status (auto-created PENDING profile on registration)
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(access);
        ResponseEntity<JsonNode> kyc = rest.exchange("/api/v1/kyc", HttpMethod.GET,
                new HttpEntity<>(headers), JsonNode.class);
        assertThat(kyc.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(kyc.getBody().get("status").asText()).isEqualTo("PENDING");

        // Refresh rotates the refresh token
        ResponseEntity<JsonNode> refreshed = rest.postForEntity("/api/v1/auth/refresh",
                Map.of("refreshToken", refresh), JsonNode.class);
        assertThat(refreshed.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(refreshed.getBody().get("refreshToken").asText()).isNotEqualTo(refresh);
    }

    @Test
    void protected_endpoint_rejects_anonymous() {
        ResponseEntity<JsonNode> resp = rest.getForEntity("/api/v1/kyc", JsonNode.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void health_is_public() {
        ResponseEntity<JsonNode> resp = rest.getForEntity("/api/v1/health", JsonNode.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().get("status").asText()).isEqualTo("UP");
    }
}
