package ma.daret.identity.kyc;

import ma.daret.identity.kyc.http.HttpKycProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(HttpKycProperties.class)
public class KycConfig {
}
