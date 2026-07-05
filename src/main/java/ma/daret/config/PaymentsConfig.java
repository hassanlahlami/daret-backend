package ma.daret.config;

import ma.daret.payments.http.HttpPaymentProperties;
import ma.daret.payments.mock.MockPaymentProperties;
import ma.daret.payments.webhook.WebhookProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({MockPaymentProperties.class, HttpPaymentProperties.class, WebhookProperties.class})
public class PaymentsConfig {
}
