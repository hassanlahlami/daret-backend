package ma.daret.config;

import ma.daret.risk.RiskProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(RiskProperties.class)
public class RiskConfig {
}
