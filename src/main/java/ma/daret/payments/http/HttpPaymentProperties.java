package ma.daret.payments.http;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Connection settings for a real HTTP PSP adapter (CMI / PayZone / Naps / …).
 * All values come from the environment; nothing is hard-coded. Paths default to
 * a generic REST shape — adapt them to the chosen provider's API.
 */
@ConfigurationProperties(prefix = "daret.payments.http")
public record HttpPaymentProperties(
        String baseUrl,
        String apiKey,
        String collectPath,
        String disbursePath,
        String statusPath
) {
    public HttpPaymentProperties {
        baseUrl = baseUrl == null ? "" : baseUrl;
        apiKey = apiKey == null ? "" : apiKey;
        collectPath = collectPath == null ? "/charges" : collectPath;
        disbursePath = disbursePath == null ? "/transfers" : disbursePath;
        statusPath = statusPath == null ? "/transactions/{ref}" : statusPath;
    }
}
