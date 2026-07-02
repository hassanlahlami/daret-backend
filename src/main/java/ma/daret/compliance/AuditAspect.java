package ma.daret.compliance;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import ma.daret.identity.auth.AuthPrincipal;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;

/**
 * Emits an {@link AuditEvent} after any {@link Audited} service method returns
 * successfully (spec §13). Resolves the actor from the security context and
 * serializes a PII-masked payload of the arguments.
 */
@Aspect
@Component
public class AuditAspect {

    private static final Logger log = LoggerFactory.getLogger(AuditAspect.class);

    private final AuditService auditService;
    private final ObjectMapper auditObjectMapper;

    public AuditAspect(AuditService auditService,
                       @Qualifier("auditObjectMapper") ObjectMapper auditObjectMapper) {
        this.auditService = auditService;
        this.auditObjectMapper = auditObjectMapper;
    }

    @Around("@annotation(audited)")
    public Object audit(ProceedingJoinPoint pjp, Audited audited) throws Throwable {
        Object result = pjp.proceed();
        try {
            String actor = currentActor();
            String payload = serializeArgs(pjp);
            String target = resolveTarget(result);
            auditService.append(actor, audited.value(), target, payload);
        } catch (Exception e) {
            // Auditing must never break the business operation; log and move on.
            log.warn("Failed to write audit event for {}: {}", audited.value(), e.getMessage());
        }
        return result;
    }

    private static String currentActor() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return "system";
        }
        if (auth.getPrincipal() instanceof AuthPrincipal p) {
            return p.userId().toString();
        }
        return "anonymous";
    }

    private String serializeArgs(ProceedingJoinPoint pjp) {
        MethodSignature sig = (MethodSignature) pjp.getSignature();
        Method method = sig.getMethod();
        String[] names = sig.getParameterNames();
        Object[] args = pjp.getArgs();

        var map = new java.util.LinkedHashMap<String, Object>();
        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            if (arg == null || isNoise(arg)) {
                continue;
            }
            map.put(names != null ? names[i] : "arg" + i, arg);
        }
        try {
            return auditObjectMapper.writeValueAsString(map);
        } catch (Exception e) {
            return "{}";
        }
    }

    /** Skip framework/context arguments that are not part of the business intent. */
    private static boolean isNoise(Object arg) {
        return arg instanceof Authentication
                || arg instanceof AuthPrincipal
                || arg instanceof Pageable
                || arg instanceof HttpServletRequest;
    }

    /** Best-effort target: the result's id, else the first UUID argument. */
    private static String resolveTarget(Object result) {
        UUID id = idOf(result);
        return id != null ? id.toString() : null;
    }

    private static UUID idOf(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            Method getId = obj.getClass().getMethod("getId");
            Object v = getId.invoke(obj);
            if (v instanceof UUID u) {
                return u;
            }
        } catch (Exception ignored) {
            // records expose components as methods; try common ones
            for (String name : List.of("id", "circleId")) {
                try {
                    Object v = obj.getClass().getMethod(name).invoke(obj);
                    if (v instanceof UUID u) {
                        return u;
                    }
                } catch (Exception ignore) {
                    // keep trying
                }
            }
        }
        return null;
    }
}
