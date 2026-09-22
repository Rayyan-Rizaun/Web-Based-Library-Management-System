package com.lms.user;

/**
 * Thrown by {@link RegistrationService} when the email or NIC submitted is
 * already on file. Carries which form field the error belongs to, so
 * {@link RegistrationController} can attach it to that exact field via
 * {@code BindingResult.rejectValue} and re-render the form — the same
 * "inline error in --overdue" experience as any other validation failure,
 * rather than a generic error page for something that is really just a
 * form mistake.
 */
public class DuplicateRegistrationException extends RuntimeException {

    private final String field;

    public DuplicateRegistrationException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String field() {
        return field;
    }
}
