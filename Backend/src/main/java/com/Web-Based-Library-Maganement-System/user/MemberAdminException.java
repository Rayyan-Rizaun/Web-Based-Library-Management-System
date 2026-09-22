package com.lms.user;

/**
 * Thrown by {@link MemberAdminService} for every refusal that is not a
 * stale id: deciding a registration that is not pending, reactivating a
 * member that was never staff-suspended, or suspending a member who still
 * has active loans. Shown as a flash-attribute error toast, never a stack
 * trace.
 */
public class MemberAdminException extends RuntimeException {

    public MemberAdminException(String message) {
        super(message);
    }
}
