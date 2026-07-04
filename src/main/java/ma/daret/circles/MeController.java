package ma.daret.circles;

import ma.daret.circles.ScheduleDtos.ContributionView;
import ma.daret.circles.ScheduleDtos.PayoutView;
import ma.daret.identity.auth.AuthPrincipal;
import ma.daret.social.VouchService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** The caller's own obligations, payouts, and social actions (spec §11, §19). */
@RestController
@RequestMapping("/api/v1/me")
@PreAuthorize("hasRole('MEMBER')")
public class MeController {

    private final ScheduleService scheduleService;
    private final VouchService vouchService;

    public MeController(ScheduleService scheduleService, VouchService vouchService) {
        this.scheduleService = scheduleService;
        this.vouchService = vouchService;
    }

    @GetMapping("/obligations")
    public List<ContributionView> obligations(@AuthenticationPrincipal AuthPrincipal principal) {
        return scheduleService.obligations(principal.userId());
    }

    @GetMapping("/payouts")
    public List<PayoutView> payouts(@AuthenticationPrincipal AuthPrincipal principal) {
        return scheduleService.payoutsForUser(principal.userId());
    }

    @GetMapping("/vouches")
    public Map<String, Long> vouches(@AuthenticationPrincipal AuthPrincipal principal) {
        return Map.of("received", vouchService.vouchesFor(principal.userId()));
    }

    /** The caller vouches for another member (trust signal → their score). */
    @PostMapping("/vouch/{userId}")
    public Map<String, Object> vouch(@AuthenticationPrincipal AuthPrincipal principal,
                                     @PathVariable UUID userId) {
        vouchService.vouch(principal.userId(), userId);
        return Map.of("ok", true);
    }
}
