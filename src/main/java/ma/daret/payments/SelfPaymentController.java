package ma.daret.payments;

import ma.daret.identity.auth.AuthPrincipal;
import ma.daret.payments.outbox.OutboxRelay;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

/** Member-initiated early payment of an upcoming contribution (E6). */
@RestController
@RequestMapping("/api/v1/me/obligations")
@PreAuthorize("hasRole('MEMBER')")
public class SelfPaymentController {

    private final CollectionService collectionService;
    private final OutboxRelay relay;

    public SelfPaymentController(CollectionService collectionService, OutboxRelay relay) {
        this.collectionService = collectionService;
        this.relay = relay;
    }

    @PostMapping("/{contributionId}/pay")
    public Map<String, Object> payNow(@AuthenticationPrincipal AuthPrincipal principal,
                                      @PathVariable UUID contributionId) {
        collectionService.payNow(contributionId, principal.userId());
        // Drain the outbox so an immediate-settlement provider clears it now; an async
        // PSP leaves it PENDING for the webhook / reconciler.
        relay.relayPending();
        return Map.of("ok", true);
    }
}
