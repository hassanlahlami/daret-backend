package ma.daret.config;

import ma.daret.payments.mock.MockPaymentProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(MockPaymentProperties.class)
public class PaymentsConfig {
}
