package ma.daret.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import ma.daret.common.web.ApiError;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Lightweight fixed-window rate limiter for {@code /api/v1/auth/*}, keyed by
 * client IP. In-memory and per-instance — adequate for MVP; swap for a shared
 * store (Redis/bucket4j) when running multi-instance under real load.
 */
@Component
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private final AuthRateLimitProperties props;
    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    public AuthRateLimitFilter(AuthRateLimitProperties props, ObjectMapper objectMapper) {
        this.props = props;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/v1/auth/");
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        if (allowed(clientKey(request))) {
            chain.doFilter(request, response);
            return;
        }
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(),
                ApiError.of("RATE_LIMITED", "Too many requests, slow down"));
    }

    private boolean allowed(String key) {
        long nowWindow = System.currentTimeMillis() / (props.windowSeconds() * 1000L);
        Window w = windows.compute(key, (k, existing) -> {
            if (existing == null || existing.epoch != nowWindow) {
                return new Window(nowWindow);
            }
            return existing;
        });
        return w.count.incrementAndGet() <= props.maxRequests();
    }

    private static String clientKey(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private static final class Window {
        private final long epoch;
        private final AtomicInteger count = new AtomicInteger(0);

        private Window(long epoch) {
            this.epoch = epoch;
        }
    }
}
