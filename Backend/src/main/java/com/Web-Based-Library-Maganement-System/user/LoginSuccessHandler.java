package com.lms.user;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.lms.common.security.AppUserPrincipal;

/**
 * The role-appropriate landing redirect after login.
 *
 * <p>Staff — anyone holding "Library Administrator", "Librarian" or
 * "Finance Officer" — lands on {@code /} (the operational dashboard,
 * {@code com.lms.common.web.HomeController}), which is built for exactly
 * that audience: library-wide loan, fine and reservation figures. Everyone
 * else — a plain member, or a self-registered account still awaiting
 * approval and holding no role at all — lands on {@code /account}, this
 * package's own page, which shows only their own membership. This is a
 * choice of destination, not a permission check: it does not restrict who
 * may later navigate to {@code /} themselves; see {@code SecurityConfig}.
 */
@Component
public class LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private static final String[] STAFF_ROLES = {"Library Administrator", "Librarian", "Finance Officer"};

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {
        boolean isStaff = authentication.getPrincipal() instanceof AppUserPrincipal principal
                && principal.hasAnyRole(STAFF_ROLES);
        setDefaultTargetUrl(isStaff ? "/" : "/account");
        super.onAuthenticationSuccess(request, response, authentication);
    }
}
