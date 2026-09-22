package com.lms.user;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Serves the login page itself. The actual POST /login is handled entirely
 * by Spring Security's form-login filter (see {@code SecurityConfig}) — no
 * controller method for it, the same way Spring Security is normally used.
 */
@Controller
public class LoginController {

    @GetMapping("/login")
    public String showLoginForm(Model model) {
        model.addAttribute("pageTitle", "Sign in");
        return "user/login";
    }
}
