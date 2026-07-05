package ma.daret.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String BEARER = "bearer-jwt";

    @Bean
    public OpenAPI daretOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Daret API")
                        .description("Digital rotating-savings (ROSCA/daret) platform — circles, economics, "
                                + "mocked money & ledger, risk & matching, compliance (audit chain, KYC/AML, "
                                + "CNDP data rights), and social trust. Modular monolith.")
                        .version("1.0.0-rc")
                        .license(new License().name("Proprietary")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER))
                .components(new Components().addSecuritySchemes(BEARER,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
