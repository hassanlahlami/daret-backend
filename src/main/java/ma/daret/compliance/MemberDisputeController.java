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

/** A member raises and views their own disputes (spec E6). */
@RestController
@RequestMapping("/api/v1/me/disputes")
@PreAuthorize("hasRole('MEMBER')")
public class MemberDisputeController {

    private final DisputeService disputes;

    public MemberDisputeController(DisputeService disputes) {
        this.disputes = disputes;
    }

    public record RaiseDisputeRequest(UUID circleId, String subject, String description) {
    }

    public record DisputeView(UUID id, UUID circleId, String subject, String description,
                              String status, String resolution, Instant at) {
        static DisputeView from(Dispute d) {
            return new DisputeView(d.getId(), d.getCircleId(), d.getSubject(), d.getDescription(),
                    d.getStatus().name(), d.getResolution(), d.getCreatedAt());
        }
    }

    @PostMapping
    public DisputeView raise(@AuthenticationPrincipal AuthPrincipal principal,
                             @RequestBody RaiseDisputeRequest req) {
        return DisputeView.from(disputes.raise(principal.userId(), req.circleId(), req.subject(), req.description()));
    }

    @GetMapping
    public List<DisputeView> mine(@AuthenticationPrincipal AuthPrincipal principal) {
        return disputes.mine(principal.userId()).stream().map(DisputeView::from).toList();
    }
}
