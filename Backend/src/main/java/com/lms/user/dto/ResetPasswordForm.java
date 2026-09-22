package com.lms.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import com.lms.common.validation.FieldsMatch;

import lombok.Getter;
import lombok.Setter;

/** Step 2 of password reset: the token (carried as a hidden field) plus the new password. */
@Getter
@Setter
@FieldsMatch(first = "newPassword", second = "confirmPassword", message = "Passwords do not match")
public class ResetPasswordForm {

    @NotBlank
    private String token;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 72, message = "Password must be at least 8 characters")
    private String newPassword;

    @NotBlank(message = "Please confirm your password")
    private String confirmPassword;
}
