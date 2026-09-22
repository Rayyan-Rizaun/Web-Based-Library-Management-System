package com.lms.user;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.lms.common.security.AppUserPrincipal;

/**
 * "My Account" — the landing page {@code LoginSuccessHandler} sends
 * anyone without a staff role to. Requires only {@code isAuthenticated()}
 * (see {@code SecurityConfig}): a member whose registration is still
 * awaiting approval must be able to see that status, which is the whole
 * point of the page existing.
 */
@Controller
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/account")
    public String show(@AuthenticationPrincipal AppUserPrincipal principal, Model model) {
        model.addAttribute("pageTitle", "My Account");
        model.addAttribute("account", accountService.viewFor(principal.userId()));
        return "user/account";
    }
}
