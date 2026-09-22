package com.lms.common.security;

import java.time.Duration;

import org.springframework.http.HttpMethod;

import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.boot.web.servlet.server.Session;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import com.lms.user.LoginFailureHandler;
import com.lms.user.LoginSuccessHandler;

/**
 * UC-01 authentication and access control.
 *
 * <p><b>Login.</b> Form login against {@link AppUserDetailsService} /
 * {@link AppUserPrincipal}, BCrypt password hashing ({@link
 * #passwordEncoder()} — the same {@code BCryptPasswordEncoder} default
 * strength {@code PasswordHashGenerator} and {@code
 * database/03_demo_passwords.sql} already use, so seeded demo accounts log
 * in correctly). The username parameter is {@code email}, matching {@code
 * AppUser}'s alternate key and {@code login.html}'s field name.
 *
 * <p><b>{@code hideUserNotFoundExceptions(false)}</b> on the {@link
 * DaoAuthenticationProvider} is a deliberate trade-off, not an oversight.
 * Spring Security's default hides the difference between "no such account"
 * and "wrong password" behind one generic {@code BadCredentialsException},
 * specifically to stop an attacker from using the login form to discover
 * which emails are registered. This project accepts that trade-off so
 * {@link LoginFailureHandler} can record the real reason —
 * {@code FailureReason.UnknownEmail} versus {@code BadPassword} — because
 * the assignment asks for that distinction in {@code FailedLoginAttempt},
 * and this is an internal institutional system rather than a public
 * consumer product where username enumeration is the primary threat.
 * {@code login.html} still shows the same generic message for both cases
 * on screen — only the stored audit record is more specific than the page.
 *
 * <p><b>Lockout and audit</b> are business logic, not framework wiring, so
 * they live in {@code com.lms.user} ({@code LoginAttemptService}, {@link
 * LoginFailureHandler}) and {@link AuditAspect}, not here.
 *
 * <p><b>"Remember me" is off</b> — simply by never calling {@code
 * http.rememberMe(...)}. There is no persistent login cookie; every
 * session ends at browser close or the idle timeout below, whichever is
 * first.
 *
 * <p><b>Session timeout</b> (30 minutes idle, business-rules.md §8) is a
 * servlet-container setting, not something {@code HttpSecurity} configures
 * — it would normally be {@code server.servlet.session.timeout} in
 * application.yml, but that file's connection settings are left alone in
 * this project, so it is set the equally-standard Spring Boot way instead:
 * a {@link WebServerFactoryCustomizer} bean.
 *
 * <p><b>Method security</b> ({@code @EnableMethodSecurity}) turns on
 * {@code @PreAuthorize} for every {@code com.lms.*} service.
 *
 * <p><b>CSRF</b> stays ON (Spring Security's default, session-based token
 * repository) — unchanged from this project's earlier placeholder
 * configuration; see {@code CurrentPathAdvice} for the one sharp edge it
 * has on a long page and how that is already handled.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final AppUserDetailsService appUserDetailsService;
    private final LoginSuccessHandler loginSuccessHandler;
    private final LoginFailureHandler loginFailureHandler;

    public SecurityConfig(AppUserDetailsService appUserDetailsService, LoginSuccessHandler loginSuccessHandler,
            LoginFailureHandler loginFailureHandler) {
        this.appUserDetailsService = appUserDetailsService;
        this.loginSuccessHandler = loginSuccessHandler;
        this.loginFailureHandler = loginFailureHandler;
    }

    /** BCrypt, default strength (10) — matches PasswordHashGenerator exactly. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider(PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(appUserDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        // See the class javadoc: deliberately not the default.
        provider.setHideUserNotFoundExceptions(false);
        return provider;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, DaoAuthenticationProvider authenticationProvider)
            throws Exception {
        http
            .authenticationProvider(authenticationProvider)
            .authorizeHttpRequests(auth -> auth
                // Static assets and the public UC-01 pages — reachable
                // without logging in, per this task's explicit scope for
                // /register and per basic usability for the others (you
                // cannot log in from a page you must already be logged in
                // to see).
                .requestMatchers(
                        "/css/**", "/js/**", "/fonts/**", "/icons.svg", "/favicon.ico",
                        "/login", "/register", "/register/**",
                        "/forgot-password", "/forgot-password/**",
                        "/reset-password", "/reset-password/**",
                        "/access-denied", "/error")
                    .permitAll()
                // UC-02 / PB-22, PB-23: a guest browses and searches the
                // catalogue and views one book's detail (with its copies,
                // availability and shelf location) without logging in —
                // GET only, and the detail page's id constrained to digits
                // ({id:[0-9]+}) so this pattern matches "/catalogue/books/42"
                // but not "/catalogue/books/new" or ".../{id}/edit", which
                // both stay behind the authenticated()+@PreAuthorize rule
                // below like every other catalogue write/management screen.
                // See com.lms.catalogue.BookService's class javadoc for the
                // matching @PreAuthorize split.
                .requestMatchers(HttpMethod.GET,
                        "/catalogue/books", "/catalogue/books/{id:[0-9]+}")
                    .permitAll()
                // Everything else — including "/" — requires a signed-in
                // user. Finer-grained, role-specific rules live as
                // @PreAuthorize on the service methods that need them
                // (this task's explicit instruction) and, for the
                // sidebar's own navigation, as th:if conditions reading
                // ${currentRoleNames} (CurrentUserAdvice) — see
                // components/sidebar.html.
                .anyRequest().authenticated())
            .formLogin(form -> form
                .loginPage("/login")
                .usernameParameter("email")
                .passwordParameter("password")
                .successHandler(loginSuccessHandler)
                .failureHandler(loginFailureHandler)
                .permitAll())
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll())
            .exceptionHandling(exceptions -> exceptions
                .accessDeniedPage("/access-denied"));
        return http.build();
    }

    /** business-rules.md §8: 30 minutes idle. See the class javadoc for why this, not application.yml. */
    @Bean
    public WebServerFactoryCustomizer<ConfigurableServletWebServerFactory> sessionTimeoutCustomizer() {
        return factory -> {
            Session session = new Session();
            session.setTimeout(Duration.ofMinutes(30));
            factory.setSession(session);
        };
    }
}
