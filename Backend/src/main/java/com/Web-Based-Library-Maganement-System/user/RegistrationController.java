package com.lms.user;

import jakarta.validation.Valid;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.lms.user.dto.RegistrationForm;

/**
 * Public self-registration (backlog item PB-21). Reachable without
 * logging in — see {@code SecurityConfig}'s {@code permitAll} matcher for
 * {@code /register**}.
 */
@Controller
public class RegistrationController {

    private final RegistrationService registrationService;

    public RegistrationController(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @GetMapping("/register")
    public String showForm(Model model) {
        if (!model.containsAttribute("registrationForm")) {
            model.addAttribute("registrationForm", new RegistrationForm());
        }
        model.addAttribute("pageTitle", "Create an account");
        return "user/register";
    }

    @PostMapping("/register")
    public String submit(@Valid @ModelAttribute("registrationForm") RegistrationForm form,
            BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("pageTitle", "Create an account");
            return "user/register";
        }

        try {
            registrationService.register(form);
        } catch (DuplicateRegistrationException e) {
            bindingResult.rejectValue(e.field(), "duplicate", e.getMessage());
            model.addAttribute("pageTitle", "Create an account");
            return "user/register";
        }

        redirectAttributes.addFlashAttribute("flashSuccessTitle", "Account created");
        redirectAttributes.addFlashAttribute("flashSuccessMessage",
                "You can sign in now. Borrowing and reservations unlock once a librarian approves your membership.");
        return "redirect:/login";
    }
}
