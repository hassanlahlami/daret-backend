package ma.daret.seed;

import ma.daret.circles.CircleActivationService;
import ma.daret.circles.CircleDtos.CircleView;
import ma.daret.circles.CircleDtos.CreateCircleRequest;
import ma.daret.circles.CircleService;
import ma.daret.circles.SlotPreference;
import ma.daret.identity.auth.AuthDtos.RegisterRequest;
import ma.daret.identity.auth.AuthService;
import ma.daret.identity.kyc.KycService;
import ma.daret.identity.kyc.KycStatus;
import ma.daret.identity.user.Role;
import ma.daret.identity.user.User;
import ma.daret.identity.user.UserRepository;
import ma.daret.payments.CyclePipeline;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Seeds a demo dataset for manual testing (spec §16): verified members, an admin
 * and a compliance officer, a FORMING circle open to join, and an ACTIVE circle
 * driven mid-lifecycle (some cycles settled, later ones still due).
 *
 * <p>Idempotent and gated by {@code daret.seed.enabled} — never runs in prod.
 * Built entirely through the real services so the ledger and audit chain stay
 * consistent.
 */
@Component
@ConditionalOnProperty(value = "daret.seed.enabled", havingValue = "true")
public class DemoDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);
    private static final String PASSWORD = "password123";

    private final AuthService authService;
    private final KycService kycService;
    private final UserRepository users;
    private final CircleService circleService;
    private final CircleActivationService activationService;
    private final CyclePipeline cyclePipeline;

    public DemoDataSeeder(AuthService authService,
                          KycService kycService,
                          UserRepository users,
                          CircleService circleService,
                          CircleActivationService activationService,
                          CyclePipeline cyclePipeline) {
        this.authService = authService;
        this.kycService = kycService;
        this.users = users;
        this.circleService = circleService;
        this.activationService = activationService;
        this.cyclePipeline = cyclePipeline;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (users.findByEmail("demo1@daret.demo").isPresent()) {
            log.info("Demo data already present — skipping seed");
            return;
        }
        log.info("Seeding demo data...");

        UUID d1 = createVerifiedMember("demo1@daret.demo");
        UUID d2 = createVerifiedMember("demo2@daret.demo");
        UUID d3 = createVerifiedMember("demo3@daret.demo");
        UUID d4 = createVerifiedMember("demo4@daret.demo");
        createVerifiedMember("demo5@daret.demo");
        grantRole("admin@daret.demo", Role.ADMIN);
        grantRole("compliance@daret.demo", Role.COMPLIANCE_OFFICER);

        // A FORMING circle, half-filled, open for manual joins.
        CircleView forming = circleService.create(d1, new CreateCircleRequest(
                "Family daret (forming)", new BigDecimal("500.00"), 4, 30, true, null, SlotPreference.FLEXIBLE));
        circleService.join(forming.id(), d2, SlotPreference.LATE);
        log.info("Seeded FORMING circle {} (2/4 seats)", forming.id());

        // A COMPLETED circle — created and fully settled first, so the later
        // partial run cannot touch it (the pipeline only acts on ACTIVE circles).
        CircleView completed = circleService.create(d1, new CreateCircleRequest(
                "Friends daret (completed)", new BigDecimal("300.00"), 3, 7, false, null, SlotPreference.FLEXIBLE));
        circleService.join(completed.id(), d2, SlotPreference.EARLY);
        circleService.join(completed.id(), d3, SlotPreference.LATE);
        activationService.activate(completed.id(), d1, true);
        cyclePipeline.run(LocalDate.now().plusDays(60)); // settle all 3 weekly cycles → COMPLETED
        log.info("Seeded COMPLETED circle {}", completed.id());

        // An ACTIVE circle, activated and driven a few cycles in (mid-lifecycle).
        CircleView active = circleService.create(d1, new CreateCircleRequest(
                "Neighbors daret (active)", new BigDecimal("1000.00"), 4, 7, false, null, SlotPreference.EARLY));
        circleService.join(active.id(), d2, SlotPreference.EARLY);
        circleService.join(active.id(), d3, SlotPreference.FLEXIBLE);
        circleService.join(active.id(), d4, SlotPreference.LATE);
        activationService.activate(active.id(), d1, true);
        // Settle the first three of four weekly cycles → mid-lifecycle.
        cyclePipeline.run(LocalDate.now().plusDays(15));
        log.info("Seeded ACTIVE circle {} (3/4 cycles settled)", active.id());

        log.info("Demo data seeded. Members demo1..demo5 / admin / compliance @daret.demo, password '{}'", PASSWORD);
    }

    private UUID createVerifiedMember(String email) {
        authService.register(new RegisterRequest(email, null, PASSWORD));
        User user = users.findByEmail(email).orElseThrow();
        kycService.decide(user.getId(), KycStatus.VERIFIED);
        return user.getId();
    }

    private void grantRole(String email, Role role) {
        User user = users.findByEmail(email).orElseGet(() -> {
            authService.register(new RegisterRequest(email, null, PASSWORD));
            User created = users.findByEmail(email).orElseThrow();
            kycService.decide(created.getId(), KycStatus.VERIFIED);
            return created;
        });
        user.addRole(role);
        users.save(user);
        log.info("Granted {} to {}", role, email);
    }
}
