package org.link.linkvault.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.link.linkvault.dto.PrivacyPolicyResponseDto;
import org.link.linkvault.entity.User;
import org.link.linkvault.repository.UserRepository;
import org.link.linkvault.service.PrivacyPolicyService;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PrivacyConsentFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;
    private final PrivacyPolicyService privacyPolicyService;

    private static final List<String> EXCLUDED_PATHS = Arrays.asList(
            "/privacy-consent", "/login", "/register", "/logout", "/error"
    );

    private static final List<String> EXCLUDED_PREFIXES = Arrays.asList(
            "/css/", "/js/", "/api/", "/h2-console/"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();

        // Skip excluded paths
        for (String excluded : EXCLUDED_PATHS) {
            if (path.equals(excluded)) {
                filterChain.doFilter(request, response);
                return;
            }
        }
        for (String prefix : EXCLUDED_PREFIXES) {
            if (path.startsWith(prefix)) {
                filterChain.doFilter(request, response);
                return;
            }
        }

        // Only check authenticated users
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            filterChain.doFilter(request, response);
            return;
        }

        // Check consent
        PrivacyPolicyResponseDto activePolicy = privacyPolicyService.getActivePolicy();
        if (activePolicy != null) {
            User user = userRepository.findByUsername(auth.getName()).orElse(null);
            if (user != null) {
                Integer agreedVersion = user.getPrivacyAgreedVersion();
                if (agreedVersion == null || agreedVersion != activePolicy.getVersion()) {
                    response.sendRedirect("/privacy-consent");
                    return;
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}
