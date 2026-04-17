package com.example.demo.config;

import com.example.demo.dto.PasskeyDto;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.webauthn.api.CredentialRecord;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialUserEntity;
import org.springframework.security.web.webauthn.management.PublicKeyCredentialUserEntityRepository;
import org.springframework.security.web.webauthn.management.UserCredentialRepository;
import org.springframework.web.filter.OncePerRequestFilter;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.web.IWebExchange;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

import java.io.IOException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.Locale;

/**
 * Intercepts GET /webauthn/register before Spring Security's default page-generating
 * filter and renders the application-styled Thymeleaf template instead.
 * <p>
 * This filter is NOT a Spring bean to avoid Spring Boot auto-registering it
 * at the servlet level; it is wired directly into the security filter chain
 * via {@code SecurityConfig}.
 */
public class PasskeyPageFilter extends OncePerRequestFilter {

    private static final String REGISTER_PATH = "/webauthn/register";
    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.systemDefault());

    private final ITemplateEngine templateEngine;
    private final PublicKeyCredentialUserEntityRepository userEntityRepository;
    private final UserCredentialRepository credentialRepository;

    public PasskeyPageFilter(ITemplateEngine templateEngine,
                             PublicKeyCredentialUserEntityRepository userEntityRepository,
                             UserCredentialRepository credentialRepository) {
        this.templateEngine = templateEngine;
        this.userEntityRepository = userEntityRepository;
        this.credentialRepository = credentialRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        if (!"GET".equalsIgnoreCase(request.getMethod())
                || !REGISTER_PATH.equals(request.getServletPath())) {
            filterChain.doFilter(request, response);
            return;
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        List<PasskeyDto> passkeys = loadPasskeys(auth.getName());

        JakartaServletWebApplication webApp =
                JakartaServletWebApplication.buildApplication(request.getServletContext());
        IWebExchange webExchange = webApp.buildExchange(request, response);
        Locale locale = request.getLocale() != null ? request.getLocale() : Locale.getDefault();
        WebContext ctx = new WebContext(webExchange, locale);
        ctx.setVariable("passkeys", passkeys);
        ctx.setVariable("activePage", "passkeys");
        ctx.setVariable("contextPath", request.getContextPath());

        String html = templateEngine.process("webauthn/register", ctx);
        response.setContentType("text/html;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(html);
    }

    private List<PasskeyDto> loadPasskeys(String username) {
        PublicKeyCredentialUserEntity entity = userEntityRepository.findByUsername(username);
        if (entity == null) {
            return List.of();
        }
        return credentialRepository.findByUserId(entity.getId())
                .stream()
                .map(this::toDto)
                .toList();
    }

    private PasskeyDto toDto(CredentialRecord record) {
        String credId = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(record.getCredentialId().getBytes());
        String created  = DATE_FMT.format(record.getCreated());
        String lastUsed = DATE_FMT.format(record.getLastUsed());
        return new PasskeyDto(record.getLabel(), credId, created, lastUsed);
    }
}
