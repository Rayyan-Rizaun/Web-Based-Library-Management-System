package com.lms.common.security;

import java.lang.reflect.Method;
import java.util.regex.Pattern;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.lms.common.domain.AppUser;
import com.lms.common.domain.AppUserRepository;
import com.lms.common.domain.AuditLog;
import com.lms.common.domain.AuditLogRepository;

/**
 * Writes one {@code AuditLog} row every time an {@link AuditAction}-annotated
 * method returns successfully — create, update, delete, or a specific
 * decision such as a fine waiver.
 *
 * <p><b>Finding the entity id without per-method configuration.</b> Every
 * entity in {@code com.lms.common.domain} names its primary-key getter
 * {@code get<Field>Id()} — {@code getUserId()}, {@code getFineId()},
 * {@code getStaffId()}, and so on (see that package's own javadoc). This
 * aspect uses that one consistent convention rather than asking each
 * {@code @AuditAction} to say where its id is: it reflects over the
 * method's return value first (the natural case for a CREATE, which
 * typically returns the saved entity), then falls back to the method's
 * arguments (the natural case for an UPDATE or DELETE, which typically
 * takes the id or the entity being changed). If neither yields one,
 * {@code EntityID} is simply left null — a nullable column, so this never
 * blocks the audit row from being written.
 *
 * <p>{@code AuditLog.UserID} comes from the security context, not a method
 * parameter, so a service method never has to thread "who is doing this"
 * through its own signature just for auditing. It is null for actions
 * taken by an anonymous caller — self-registration is exactly this case,
 * and the schema already allows it ("NULL for system or trigger actions").
 */
@Aspect
@Component
public class AuditAspect {

    private static final Logger log = LoggerFactory.getLogger(AuditAspect.class);
    private static final Pattern ID_GETTER = Pattern.compile("get[A-Z]\\w*Id");

    private final AuditLogRepository auditLogRepository;
    private final AppUserRepository appUserRepository;

    public AuditAspect(AuditLogRepository auditLogRepository, AppUserRepository appUserRepository) {
        this.auditLogRepository = auditLogRepository;
        this.appUserRepository = appUserRepository;
    }

    @AfterReturning(pointcut = "@annotation(auditAction)", returning = "result")
    public void audit(JoinPoint joinPoint, AuditAction auditAction, Object result) {
        try {
            AuditLog entry = new AuditLog();
            entry.setUser(currentUser());
            entry.setActionName(auditAction.action());
            entry.setEntityName(auditAction.entity());
            entry.setEntityId(findEntityId(result, joinPoint.getArgs()));
            auditLogRepository.save(entry);
        } catch (RuntimeException e) {
            // The audited action already completed and, for most of these
            // methods, already committed — a logging failure must not turn
            // into a user-facing error for an operation that otherwise
            // succeeded. Loud in the server log, invisible to the caller.
            log.error("AuditAspect failed to record {} on {} (method {})",
                    auditAction.action(), auditAction.entity(), joinPoint.getSignature(), e);
        }
    }

    private AppUser currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AppUserPrincipal principal)) {
            return null;
        }
        // A reference, not a fetch: the row's own id is all AuditLog needs,
        // and this never issues a query for it (standard JPA lazy-reference).
        return appUserRepository.getReferenceById(principal.userId());
    }

    private static String findEntityId(Object result, Object[] args) {
        String fromResult = idGetterValue(result);
        if (fromResult != null) {
            return fromResult;
        }
        for (Object arg : args) {
            String fromArg = idGetterValue(arg);
            if (fromArg != null) {
                return fromArg;
            }
        }
        return null;
    }

    private static String idGetterValue(Object candidate) {
        if (candidate == null) {
            return null;
        }
        for (Method method : candidate.getClass().getMethods()) {
            if (method.getParameterCount() == 0 && ID_GETTER.matcher(method.getName()).matches()) {
                try {
                    Object id = method.invoke(candidate);
                    return id == null ? null : id.toString();
                } catch (ReflectiveOperationException e) {
                    return null;
                }
            }
        }
        return null;
    }
}
