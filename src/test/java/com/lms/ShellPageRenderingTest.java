package com.lms;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.lms.common.domain.AppUser;
import com.lms.common.domain.Role;
import com.lms.common.domain.UserRole;
import com.lms.common.security.AppUserDetailsService;
import com.lms.common.security.AppUserPrincipal;
import com.lms.common.security.SecurityConfig;
import com.lms.common.web.CurrentPathAdvice;
import com.lms.common.web.HomeController;
import com.lms.common.web.StatusPillMapper;
import com.lms.user.LoginFailureHandler;
import com.lms.user.LoginSuccessHandler;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Renders the shell page through the real Thymeleaf templates, without a
 * database: a web-layer slice test, so it runs even on a machine with no
 * SQL Server. Catches broken fragment references and template expressions
 * — the kind of mistake that otherwise only shows up as an error page.
 *
 * <p>{@code @WebMvcTest} only loads the web layer plus whatever is listed
 * in {@code @Import} — every {@code @Component} a rendered view reaches
 * for (here, {@code StatusPillMapper}, called from
 * {@code components/status-pill.html} as {@code @statusPill}) has to be
 * added here too, or the view fails with a "No bean named ..." error that
 * has nothing to do with the page itself.
 *
 * <p>{@code SecurityConfig} (UC-01) now also needs {@link
 * AppUserDetailsService}, {@link LoginSuccessHandler} and {@link
 * LoginFailureHandler} to build its {@code SecurityFilterChain} bean. This
 * test never logs in — it only renders "/" — so those three are
 * {@code @MockitoBean} stand-ins rather than real, database-backed beans:
 * enough for the filter chain to construct, with no behaviour this test
 * exercises.
 *
 * <p>{@code SecurityConfig} also now requires every request (including
 * "/") to be authenticated, so the test signs in a fake staff principal
 * itself rather than using {@code @WithMockUser} — that annotation's
 * default principal is a plain Spring Security {@code User}, not an
 * {@link AppUserPrincipal}, and {@code CurrentUserAdvice} (which
 * {@code components/sidebar.html} and {@code components/topbar.html} both
 * read) only recognises the latter; with a {@code @WithMockUser} the
 * request itself passes {@code authorizeHttpRequests}, but the page then
 * renders as if no one were signed in — an empty sidebar, "Guest" in the
 * topbar — hiding exactly what this test exists to catch. Building a real
 * {@link AppUserPrincipal} around a throwaway {@link AppUser} (with one
 * {@link UserRole}/{@link Role} granting "Library Administrator", the
 * project's verbatim-role-name authority convention) exercises the shell
 * the way an actual signed-in request would.
 */
@WebMvcTest(HomeController.class)
@Import({SecurityConfig.class, CurrentPathAdvice.class, StatusPillMapper.class})
class ShellPageRenderingTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private AppUserDetailsService appUserDetailsService;

    @MockitoBean
    private LoginSuccessHandler loginSuccessHandler;

    @MockitoBean
    private LoginFailureHandler loginFailureHandler;

    private static AppUserPrincipal fakeStaffPrincipal() {
        Role role = new Role();
        role.setRoleName("Library Administrator");

        AppUser user = new AppUser();
        user.setFirstName("Test");
        user.setLastName("Administrator");
        user.setEmail("test.administrator@nlms.lk");
        user.setPasswordHash("unused-in-this-test");

        UserRole grant = new UserRole();
        grant.setUser(user);
        grant.setRole(role);
        user.getRoleAssignments().add(grant);

        return new AppUserPrincipal(user);
    }

    @Test
    void homeRendersInsideTheBaseLayout() throws Exception {
        AppUserPrincipal principal = fakeStaffPrincipal();
        mvc.perform(get("/").with(authentication(
                        new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()))))
                .andExpect(status().isOk())
                // from layout/base.html
                .andExpect(content().string(containsString("/css/app.css")))
                .andExpect(content().string(containsString("id=\"toast-region\"")))
                // from components/sidebar.html, with the active link resolved
                .andExpect(content().string(containsString("sidebar__link is-active")))
                // from components/topbar.html
                .andExpect(content().string(containsString("id=\"theme-toggle\"")))
                // from home.html, and its <title> made it into the layout's <head>
                .andExpect(content().string(containsString("<title>Dashboard · Library Management System</title>")))
                // from home.html + templates/components/*, exercising the shared components
                .andExpect(content().string(containsString("Recent Activity")))
                .andExpect(content().string(containsString("status-pill status-pill--lost")))
                .andExpect(content().string(containsString("stat-tile stat-tile--overdue")));
    }
}
