package com.lms.common.validation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * Class-level Jakarta Bean Validation constraint: two properties on the
 * same object must be equal. Built for password-confirmation fields
 * ({@code newPassword} / {@code confirmPassword}), which Bean Validation
 * has no built-in cross-field check for.
 *
 * <pre>
 *   {@literal @}FieldsMatch(first = "password", second = "confirmPassword",
 *               message = "Passwords do not match")
 *   public class RegistrationForm { ... }
 * </pre>
 *
 * <p>The mismatch error attaches to {@code second} (so
 * {@code components/form-field.html} shows it under the confirmation
 * field, where the user is actually looking when it fails) — see
 * {@link FieldsMatchValidator}.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = FieldsMatchValidator.class)
public @interface FieldsMatch {

    String message() default "Values do not match";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    /** Bean property name of the first value, e.g. "password". */
    String first();

    /** Bean property name of the second value, e.g. "confirmPassword" — also where the error is reported. */
    String second();
}
