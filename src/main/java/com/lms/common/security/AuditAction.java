package com.lms.common.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a service method whose successful completion belongs in
 * {@code AuditLog} — "track who performed each action" (security NFR).
 * {@link AuditAspect} does the actual writing; this annotation only says
 * what to write.
 *
 * <pre>
 *   {@literal @}AuditAction(action = "CREATE", entity = "AppUser")
 *   public AppUser register(RegistrationForm form) { ... }
 * </pre>
 *
 * <p>Applies to any create, update, delete — or a specific business
 * decision worth its own audit trail even though it is technically an
 * update, such as waiving a fine:
 * {@code @AuditAction(action = "WAIVE", entity = "Fine")} on whichever
 * {@code com.lms.fine} service method sets {@code Fine.status} to
 * {@code Waived}. That package is not built in this pass — this is the
 * annotation its owner adds when it is.
 *
 * <p>Only fires when the method returns normally. A method that throws
 * (a failed create, a rejected update) is not audited as one — there is
 * nothing to say completed.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface AuditAction {

    /** e.g. "CREATE", "UPDATE", "DELETE", "WAIVE". Free text, stored verbatim in AuditLog.ActionName. */
    String action();

    /** The entity's plain name, e.g. "AppUser", "Fine" — stored in AuditLog.EntityName. */
    String entity();
}
