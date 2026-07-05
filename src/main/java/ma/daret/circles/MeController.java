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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
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
    private final CircleMemberRepository members;
    private final CircleRepository circles;
    private final ContributionRepository contributions;
    private final PayoutRepository payouts;

    public MeController(ScheduleService scheduleService, VouchService vouchService,
                        CircleMemberRepository members, CircleRepository circles,
                        ContributionRepository contributions, PayoutRepository payouts) {
        this.scheduleService = scheduleService;
        this.vouchService = vouchService;
        this.members = members;
        this.circles = circles;
        this.contributions = contributions;
        this.payouts = payouts;
    }

    public record ReceiptView(String reference, String type, UUID circleId, String circleName,
                              int cycleIndex, BigDecimal amount, Instant at) {
    }

    /** Settled financial events for the caller (billing receipts): paid
     *  contributions and received payouts, newest first. */
    @GetMapping("/receipts")
    public List<ReceiptView> receipts(@AuthenticationPrincipal AuthPrincipal principal) {
        UUID uid = principal.userId();
        List<ReceiptView> out = new ArrayList<>();
        contributions.findByUserId(uid).stream()
                .filter(c -> c.getStatus() == Contribution.Status.PAID)
                .forEach(c -> out.add(new ReceiptView("DRT-C-" + shortRef(c.getId()), "CONTRIBUTION",
                        c.getCircleId(), circleName(c.getCircleId()), c.getCycleIndex(), c.getAmount(), c.getUpdatedAt())));
        payouts.findByRecipientUserId(uid).stream()
                .filter(p -> p.getStatus() == Payout.Status.PAID)
                .forEach(p -> out.add(new ReceiptView("DRT-P-" + shortRef(p.getId()), "PAYOUT",
                        p.getCircleId(), circleName(p.getCircleId()), p.getCycleIndex(), p.getAmount(), p.getUpdatedAt())));
        out.sort(Comparator.comparing(ReceiptView::at, Comparator.nullsLast(Comparator.reverseOrder())));
        return out;
    }

    private String circleName(UUID circleId) {
        return circles.findById(circleId).map(Circle::getName).orElse(null);
    }

    private static String shortRef(UUID id) {
        return id.toString().substring(0, 8).toUpperCase();
    }

    public record MyCircleView(UUID id, String name, String status, int filledSeats, int seatCount) {
    }

    /** The circles the caller belongs to (owned or joined) — powers onboarding + "my circles". */
    @GetMapping("/circles")
    public List<MyCircleView> myCircles(@AuthenticationPrincipal AuthPrincipal principal) {
        return members.findByUserId(principal.userId()).stream()
                .map(m -> circles.findById(m.getCircleId()).orElse(null))
                .filter(c -> c != null)
                .map(c -> new MyCircleView(c.getId(), c.getName(), c.getStatus().name(),
                        members.countByCircleId(c.getId()), c.getSeatCount()))
                .toList();
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
