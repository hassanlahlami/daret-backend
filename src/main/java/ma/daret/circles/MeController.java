package ma.daret.circles;

import ma.daret.circles.ScheduleDtos.ContributionView;
import ma.daret.circles.ScheduleDtos.PayoutView;
import ma.daret.identity.auth.AuthPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** The caller's own obligations and payouts (spec §11: /me/obligations, /me/payouts). */
@RestController
@RequestMapping("/api/v1/me")
@PreAuthorize("hasRole('MEMBER')")
public class MeController {

    private final ScheduleService scheduleService;

    public MeController(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    @GetMapping("/obligations")
    public List<ContributionView> obligations(@AuthenticationPrincipal AuthPrincipal principal) {
        return scheduleService.obligations(principal.userId());
    }

    @GetMapping("/payouts")
    public List<PayoutView> payouts(@AuthenticationPrincipal AuthPrincipal principal) {
        return scheduleService.payoutsForUser(principal.userId());
    }
}
