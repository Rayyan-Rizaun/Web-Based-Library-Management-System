package com.lms.common.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Two small pieces of request prep every page needs, applied before any
 * view starts rendering (both are {@code @ModelAttribute} methods, which
 * Spring MVC runs ahead of the handler method and therefore well ahead of
 * the view).
 */
@ControllerAdvice
public class CurrentPathAdvice {

    /**
     * Adds the current request path to every view's model as
     * {@code currentPath}, so the shared sidebar can highlight the active
     * link.
     *
     * <p>Why this exists: Thymeleaf 3.1 (the version Spring Boot 3 ships)
     * removed the {@code #request} / {@code #httpServletRequest} expression
     * objects templates used to read the URL from, for security reasons. A
     * template that still uses them fails at render time, so the path has
     * to be handed to the view explicitly.
     */
    @ModelAttribute("currentPath")
    public String currentPath(HttpServletRequest request) {
        return request.getRequestURI().substring(request.getContextPath().length());
    }

    /**
     * Forces the CSRF token to resolve now, rather than whenever the first
     * {@code <form th:action>} on the page happens to be reached.
     *
     * <p>Spring Security defers generating the token until something reads
     * it. With the default session-based repository, that first read is
     * also what creates the HTTP session — normally invisible, but on a
     * long page the response can already be streaming (chunked, no fixed
     * Content-Length) by the time a form appears, and creating a session
     * that late throws {@code IllegalStateException: Cannot create a
     * session after the response has been committed} (hit on the
     * dashboard's own confirm-dialog demo, at the bottom of a long page).
     * Reading it here, in a {@code @ModelAttribute} method, happens before
     * the view is even selected — see {@code SecurityConfig} for the fuller
     * explanation. {@code CsrfToken.class.getName()} is the same request
     * attribute key Spring's own {@code CsrfRequestDataValueProcessor}
     * reads to fill in the hidden field, so this doesn't reach into
     * anything undocumented.
     */
    @ModelAttribute
    public void resolveCsrfTokenEarly(HttpServletRequest request) {
        Object token = request.getAttribute(CsrfToken.class.getName());
        if (token instanceof CsrfToken csrfToken) {
            csrfToken.getToken();
        }
    }
}
