package com.lms.user;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.lms.user.dto.ForgotPasswordForm;
import com.lms.user.dto.ResetPasswordForm;

/**
 * UC-01 "forgot password": request a link (logged to the console, not
 * emailed — see {@link PasswordResetMailer}), then use it once. Reachable
 * without logging in — see {@code SecurityConfig}'s {@code permitAll}
 * matcher for these paths.
 */
@Controller
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    public PasswordResetController(PasswordResetService passwordResetService) {
        this.passwordResetService = passwordResetService;
    }

    @GetMapping("/forgot-password")
    public String showRequestForm(Model model) {
        if (!model.containsAttribute("forgotPasswordForm")) {
            model.addAttribute("forgotPasswordForm", new ForgotPasswordForm());
        }
        model.addAttribute("pageTitle", "Forgot password");
        return "user/forgot-password";
    }

    @PostMapping("/forgot-password")
    public String requestReset(@Valid @ModelAttribute("forgotPasswordForm") ForgotPasswordForm form,
            BindingResult bindingResult, HttpServletRequest request, Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("pageTitle", "Forgot password");
            return "user/forgot-password";
        }

        passwordResetService.requestReset(form.getEmail(), request);

        // Identical whether or not the email exists — see requestReset's javadoc.
        model.addAttribute("pageTitle", "Forgot password");
        model.addAttribute("submitted", true);
        return "user/forgot-password";
    }

    @GetMapping("/reset-password")
    public String showResetForm(@RequestParam String token, Model model) {
        model.addAttribute("pageTitle", "Reset password");
        if (!passwordResetService.isValid(token)) {
            model.addAttribute("tokenInvalid", true);
            return "user/reset-password";
        }
        ResetPasswordForm form = new ResetPasswordForm();
        form.setToken(token);
        model.addAttribute("resetPasswordForm", form);
        return "user/reset-password";
    }

    @PostMapping("/reset-password")
    public String submitReset(@Valid @ModelAttribute("resetPasswordForm") ResetPasswordForm form,
            BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
        model.addAttribute("pageTitle", "Reset password");

        if (!passwordResetService.isValid(form.getToken())) {
            model.addAttribute("tokenInvalid", true);
            return "user/reset-password";
        }
        if (bindingResult.hasErrors()) {
            return "user/reset-password";
        }

        passwordResetService.resetPassword(form.getToken(), form.getNewPassword());

        redirectAttributes.addFlashAttribute("flashSuccessTitle", "Password changed");
        redirectAttributes.addFlashAttribute("flashSuccessMessage", "Sign in with your new password.");
        return "redirect:/login";
    }
}
