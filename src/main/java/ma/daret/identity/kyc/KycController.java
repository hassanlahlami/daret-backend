package ma.daret.identity.kyc;

import jakarta.validation.Valid;
import ma.daret.identity.auth.AuthPrincipal;
import ma.daret.identity.kyc.KycDtos.KycStatusResponse;
import ma.daret.identity.kyc.KycDtos.SubmitKycRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/kyc")
@PreAuthorize("hasRole('MEMBER')")
public class KycController {

    private final KycService kycService;

    public KycController(KycService kycService) {
        this.kycService = kycService;
    }

    @PostMapping
    public KycStatusResponse submit(@AuthenticationPrincipal AuthPrincipal principal,
                                    @Valid @RequestBody SubmitKycRequest req) {
        return KycStatusResponse.from(kycService.submit(principal.userId(), req.partnerRef()));
    }

    @GetMapping
    public KycStatusResponse status(@AuthenticationPrincipal AuthPrincipal principal) {
        return KycStatusResponse.from(kycService.getForUser(principal.userId()));
    }
}
