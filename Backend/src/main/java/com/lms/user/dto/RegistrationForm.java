package com.lms.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.lms.common.validation.FieldsMatch;

import lombok.Getter;
import lombok.Setter;

/**
 * The public self-registration form (backlog item PB-21). Field names
 * match what {@code components/form-field.html} binds to in
 * {@code register.html} via {@code th:field}.
 *
 * <p>Deliberately has no field for a role, a member type, a membership
 * status, or anything else that would let a guest choose their own place
 * in the system — see {@code RegistrationService} and business-rules.md
 * §8. What a guest can submit is exactly what this class declares.
 */
@Getter
@Setter
@FieldsMatch(first = "password", second = "confirmPassword", message = "Passwords do not match")
public class RegistrationForm {

    @NotBlank(message = "First name is required")
    @Size(max = 100, message = "First name must be at most 100 characters")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 100, message = "Last name must be at most 100 characters")
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Enter a valid email address")
    @Size(max = 254, message = "Email must be at most 254 characters")
    private String email;

    /** Sri Lankan NIC: old format (9 digits + V/X) or new format (12 digits). */
    @NotBlank(message = "NIC is required")
    @Pattern(regexp = "^(\\d{9}[vVxX]|\\d{12})$", message = "Enter a valid NIC (9 digits + V, or 12 digits)")
    private String nationalId;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[0-9+ ()-]{7,20}$", message = "Enter a valid phone number")
    private String phone;

    @NotBlank(message = "Address is required")
    @Size(max = 150, message = "Address must be at most 150 characters")
    private String addressLine1;

    @Size(max = 150, message = "Address must be at most 150 characters")
    private String addressLine2;

    @NotBlank(message = "City is required")
    @Size(max = 100, message = "City must be at most 100 characters")
    private String city;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 72, message = "Password must be at least 8 characters")
    private String password;

    @NotBlank(message = "Please confirm your password")
    private String confirmPassword;
}
