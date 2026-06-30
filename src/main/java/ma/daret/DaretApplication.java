package ma.daret;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Daret — digital rotating-savings (ROSCA) platform.
 *
 * <p>Modular monolith. Phase 1 ships the {@code identity} module (auth, users, KYC)
 * plus the cross-cutting foundation (security, error handling, Flyway baseline).
 * Later phases add {@code circles}, {@code scoring}, {@code risk}, {@code ledger},
 * {@code payments}, and {@code compliance} as packages inside this same app.
 */
@SpringBootApplication
@EnableJpaAuditing
public class DaretApplication {

    public static void main(String[] args) {
        SpringApplication.run(DaretApplication.class, args);
    }
}
