package ma.daret.compliance;

import ma.daret.identity.auth.AuthPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** A member exercises their own CNDP data rights: access + erasure (spec §13 / F1). */
@RestController
@RequestMapping("/api/v1/me")
@PreAuthorize("hasRole('MEMBER')")
public class MemberDataRightsController {

    private final DataRightsService dataRights;

    public MemberDataRightsController(DataRightsService dataRights) {
        this.dataRights = dataRights;
    }

    /** Right of access: a downloadable bundle of the caller's personal data. */
    @GetMapping("/data-export")
    public DataRightsService.DataExport export(@AuthenticationPrincipal AuthPrincipal principal) {
        return dataRights.export(principal.userId());
    }

    public record ErasureRequestBody(String reason) {
    }

    public record ErasureView(UUID id, String reason, String status, String resolution, Instant at) {
        static ErasureView from(ErasureRequest r) {
            return new ErasureView(r.getId(), r.getReason(), r.getStatus().name(),
                    r.getResolution(), r.getCreatedAt());
        }
    }

    /** Right to erasure: files a request for a compliance officer to review. */
    @PostMapping("/erasure-request")
    public ErasureView requestErasure(@AuthenticationPrincipal AuthPrincipal principal,
                                      @RequestBody(required = false) ErasureRequestBody body) {
        String reason = body == null ? null : body.reason();
        return ErasureView.from(dataRights.requestErasure(principal.userId(), reason));
    }

    @GetMapping("/erasure-requests")
    public List<ErasureView> mine(@AuthenticationPrincipal AuthPrincipal principal) {
        return dataRights.mine(principal.userId()).stream().map(ErasureView::from).toList();
    }
}
