package ma.daret.waitlist;

import ma.daret.common.web.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class WaitlistService {

    private final WaitlistRepository repository;

    public WaitlistService(WaitlistRepository repository) {
        this.repository = repository;
    }

    /** Idempotent signup: re-submitting the same email is a no-op success. */
    @Transactional
    public boolean join(String email, String locale) {
        if (email == null || !email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw ApiException.badRequest("INVALID_EMAIL", "A valid email is required");
        }
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        if (repository.findByEmail(normalized).isPresent()) {
            return false; // already on the list
        }
        repository.save(new WaitlistEntry(normalized, locale));
        return true;
    }

    @Transactional(readOnly = true)
    public List<WaitlistEntry> all() {
        return repository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public long count() {
        return repository.count();
    }
}
