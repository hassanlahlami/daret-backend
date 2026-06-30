package ma.daret.circles;

import jakarta.validation.Valid;
import ma.daret.circles.CircleDtos.CircleDetailView;
import ma.daret.circles.CircleDtos.CircleView;
import ma.daret.circles.CircleDtos.CreateCircleRequest;
import ma.daret.circles.CircleDtos.JoinCircleRequest;
import ma.daret.economics.FeePreviewDtos.FeePreviewResponse;
import ma.daret.economics.FeePreviewService;
import ma.daret.identity.auth.AuthPrincipal;
import ma.daret.scoring.CreditTier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/circles")
public class CircleController {

    private final CircleService circleService;
    private final FeePreviewService feePreviewService;
    private final CircleActivationService activationService;

    public CircleController(CircleService circleService,
                            FeePreviewService feePreviewService,
                            CircleActivationService activationService) {
        this.circleService = circleService;
        this.feePreviewService = feePreviewService;
        this.activationService = activationService;
    }

    private static boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities()
                .contains(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    @PostMapping
    @PreAuthorize("hasRole('MEMBER')")
    @ResponseStatus(HttpStatus.CREATED)
    public CircleView create(@AuthenticationPrincipal AuthPrincipal principal,
                             @Valid @RequestBody CreateCircleRequest req) {
        return circleService.create(principal.userId(), req);
    }

    @GetMapping
    @PreAuthorize("hasRole('MEMBER')")
    public Page<CircleView> discover(@RequestParam(required = false) CircleStatus status,
                                     @RequestParam(required = false) BigDecimal minAmount,
                                     @RequestParam(required = false) BigDecimal maxAmount,
                                     @RequestParam(required = false) Integer cycleLengthDays,
                                     @RequestParam(required = false) Boolean shariaMode,
                                     @PageableDefault(size = 20) Pageable pageable) {
        return circleService.discover(status, minAmount, maxAmount, cycleLengthDays, shariaMode, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('MEMBER')")
    public CircleDetailView detail(@PathVariable UUID id) {
        return circleService.detail(id);
    }

    @GetMapping("/{id}/fee-preview")
    @PreAuthorize("hasRole('MEMBER')")
    public FeePreviewResponse feePreview(@PathVariable UUID id,
                                         @RequestParam int position,
                                         @RequestParam(required = false) CreditTier tier) {
        return feePreviewService.preview(id, position, tier);
    }

    @PostMapping("/{id}/join")
    @PreAuthorize("hasRole('MEMBER')")
    public CircleView join(@AuthenticationPrincipal AuthPrincipal principal,
                           @PathVariable UUID id,
                           @Valid @RequestBody JoinCircleRequest req) {
        return circleService.join(id, principal.userId(), req.slotPreference());
    }

    @PostMapping("/{id}/activate")
    @PreAuthorize("hasAnyRole('MEMBER','ADMIN')")
    public CircleActivationService.ActivationResult activate(@AuthenticationPrincipal AuthPrincipal principal,
                                                             Authentication authentication,
                                                             @PathVariable UUID id) {
        return activationService.activate(id, principal.userId(), isAdmin(authentication));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('MEMBER','ADMIN')")
    public CircleView cancel(@AuthenticationPrincipal AuthPrincipal principal,
                             Authentication authentication,
                             @PathVariable UUID id) {
        return circleService.cancel(id, principal.userId(), isAdmin(authentication));
    }
}
