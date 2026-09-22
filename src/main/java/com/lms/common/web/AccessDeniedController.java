package com.lms.common.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Serves the 403 page {@code SecurityConfig} points
 * {@code exceptionHandling().accessDeniedPage(...)} at. A separate small
 * controller, not folded into {@link HomeController}, so this change
 * touches nothing HomeController already does (CLAUDE.md rule 3).
 *
 * <p>Reached only by an authenticated user who hit {@code @PreAuthorize} or
 * a URL rule they don't satisfy — an anonymous visitor is redirected to
 * {@code /login} instead, never here — so it renders inside the normal app
 * shell with their own (role-filtered) navigation still available.
 */
@Controller
public class AccessDeniedController {

    @GetMapping("/access-denied")
    public String show(Model model) {
        model.addAttribute("pageTitle", "Access denied");
        return "error/access-denied";
    }
}
