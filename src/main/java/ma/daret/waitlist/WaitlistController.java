package ma.daret.waitlist;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/** Public marketing waitlist (G7): anyone can join; admins can read it. */
@RestController
@RequestMapping("/api/v1/waitlist")
public class WaitlistController {

    private final WaitlistService waitlist;

    public WaitlistController(WaitlistService waitlist) {
        this.waitlist = waitlist;
    }

    public record JoinRequest(String email, String locale) {
    }

    /** Public: join the waitlist. Idempotent. */
    @PostMapping
    public Map<String, Object> join(@RequestBody JoinRequest req) {
        boolean added = waitlist.join(req.email(), req.locale());
        return Map.of("ok", true, "added", added);
    }

    public record WaitlistView(String email, String locale, Instant at) {
        static WaitlistView from(WaitlistEntry e) {
            return new WaitlistView(e.getEmail(), e.getLocale(), e.getCreatedAt());
        }
    }

    /** Admin: read the waitlist. */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> list() {
        List<WaitlistView> entries = waitlist.all().stream().map(WaitlistView::from).toList();
        return Map.of("count", entries.size(), "entries", entries);
    }
}
