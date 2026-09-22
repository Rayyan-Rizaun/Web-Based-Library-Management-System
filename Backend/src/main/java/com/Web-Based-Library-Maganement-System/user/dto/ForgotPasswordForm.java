package com.lms.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import lombok.Getter;
import lombok.Setter;

/** Step 1 of password reset: "what's your email". */
@Getter
@Setter
public class ForgotPasswordForm {

    @NotBlank(message = "Email is required")
    @Email(message = "Enter a valid email address")
    private String email;
}
