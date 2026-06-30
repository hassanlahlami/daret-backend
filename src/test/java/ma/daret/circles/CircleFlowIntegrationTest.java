package ma.daret.circles;

import com.fasterxml.jackson.databind.JsonNode;
import ma.daret.identity.kyc.KycProfile;
import ma.daret.identity.kyc.KycProfileRepository;
import ma.daret.identity.kyc.KycStatus;
import ma.daret.identity.user.User;
import ma.daret.identity.user.UserRepository;
import ma.daret.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Phase-2 circles: KYC gate on creation, create→join happy path, and discovery.
 */
class CircleFlowIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    TestRestTemplate rest;
    @Autowired
    UserRepository users;
    @Autowired
    KycProfileRepository kycProfiles;

    /** Register a user and return only its access token (KYC stays PENDING). */
    private String register(String email) {
        ResponseEntity<JsonNode> reg = rest.postForEntity("/api/v1/auth/register",
                Map.of("email", email, "password", "supersecret1"), JsonNode.class);
        assertThat(reg.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return reg.getBody().get("accessToken").asText();
    }

    /** Register a user, force its KYC to VERIFIED, and return the access token. */
    private String registerVerified(String email) {
        String token = register(email);
        User user = users.findByEmail(email).orElseThrow();
        KycProfile profile = kycProfiles.findByUserId(user.getId()).orElseThrow();
        profile.setStatus(KycStatus.VERIFIED);
        kycProfiles.save(profile);
        return token;
    }

    private HttpHeaders bearer(String token) {
        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(token);
        return h;
    }

    @Test
    void unverified_user_cannot_create_circle() {
        String token = register("pending@example.ma");

        ResponseEntity<JsonNode> resp = rest.exchange("/api/v1/circles", HttpMethod.POST,
                new HttpEntity<>(Map.of("name", "Family daret", "contributionAmount", 500,
                        "seatCount", 5, "cycleLengthDays", 30, "shariaMode", true), bearer(token)),
                JsonNode.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(resp.getBody().get("code").asText()).isEqualTo("KYC_REQUIRED");
    }

    @Test
    void verified_user_creates_and_another_joins() {
        String ownerToken = registerVerified("owner@example.ma");

        ResponseEntity<JsonNode> created = rest.exchange("/api/v1/circles", HttpMethod.POST,
                new HttpEntity<>(Map.of("name", "Family daret", "contributionAmount", 500,
                        "seatCount", 3, "cycleLengthDays", 30, "shariaMode", true), bearer(ownerToken)),
                JsonNode.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(created.getBody().get("status").asText()).isEqualTo("FORMING");
        assertThat(created.getBody().get("filledSeats").asInt()).isEqualTo(1);
        String circleId = created.getBody().get("id").asText();

        // Discovery shows the FORMING circle.
        ResponseEntity<JsonNode> page = rest.exchange("/api/v1/circles", HttpMethod.GET,
                new HttpEntity<>(bearer(ownerToken)), JsonNode.class);
        assertThat(page.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(page.getBody().get("totalElements").asInt()).isGreaterThanOrEqualTo(1);

        // A second verified member joins.
        String joinerToken = registerVerified("joiner@example.ma");
        ResponseEntity<JsonNode> joined = rest.exchange("/api/v1/circles/" + circleId + "/join",
                HttpMethod.POST,
                new HttpEntity<>(Map.of("slotPreference", "EARLY"), bearer(joinerToken)), JsonNode.class);
        assertThat(joined.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(joined.getBody().get("filledSeats").asInt()).isEqualTo(2);

        // Joining twice is rejected.
        ResponseEntity<JsonNode> again = rest.exchange("/api/v1/circles/" + circleId + "/join",
                HttpMethod.POST,
                new HttpEntity<>(Map.of("slotPreference", "LATE"), bearer(joinerToken)), JsonNode.class);
        assertThat(again.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(again.getBody().get("code").asText()).isEqualTo("ALREADY_MEMBER");
    }
}
