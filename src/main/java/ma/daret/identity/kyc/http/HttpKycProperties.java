package ma.daret.identity.kyc.http;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Connection settings for a real eKYC partner adapter. All from the environment. */
@ConfigurationProperties(prefix = "daret.kyc.http")
public record HttpKycProperties(
        String baseUrl,
        String apiKey,
        String initiatePath
) {
    public HttpKycProperties {
        baseUrl = baseUrl == null ? "" : baseUrl;
        apiKey = apiKey == null ? "" : apiKey;
        initiatePath = initiatePath == null ? "/sessions" : initiatePath;
    }
}
