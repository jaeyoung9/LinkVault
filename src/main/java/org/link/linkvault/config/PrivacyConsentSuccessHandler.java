package org.link.linkvault.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.link.linkvault.dto.PrivacyPolicyResponseDto;
import org.link.linkvault.entity.User;
import org.link.linkvault.repository.UserRepository;
import org.link.linkvault.service.AccountLockoutService;
import org.link.linkvault.service.PrivacyPolicyService;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class PrivacyConsentSuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final PrivacyPolicyService privacyPolicyService;
    private final AccountLockoutService accountLockoutService;

    @Override
    @Transactional
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        String username = authentication.getName();

        // Reset lockout counters on successful login
        accountLockoutService.resetOnSuccess(username);

        User user = userRepository.findByUsername(username).orElse(null);

        if (user != null) {
            user.recordLogin();
            userRepository.save(user);

            PrivacyPolicyResponseDto activePolicy = privacyPolicyService.getActivePolicy();
            if (activePolicy != null) {
                Integer agreedVersion = user.getPrivacyAgreedVersion();
                if (agreedVersion == null || agreedVersion != activePolicy.getVersion()) {
                    log.info("User '{}' needs to consent to privacy policy v{}", username, activePolicy.getVersion());
                    response.sendRedirect("/privacy-consent");
                    return;
                }
            }
        }

        response.sendRedirect("/");
    }
}
