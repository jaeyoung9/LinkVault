package org.link.linkvault.config;

import lombok.RequiredArgsConstructor;
import org.link.linkvault.service.AccountLockoutService;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
@RequiredArgsConstructor
public class LoginFailureHandler implements AuthenticationFailureHandler {

    private final AccountLockoutService accountLockoutService;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {
        String username = request.getParameter("username");
        if (username != null && !username.isBlank()) {
            accountLockoutService.recordFailedLogin(username);
        }

        if (exception instanceof LockedException) {
            response.sendRedirect("/login?locked=true");
        } else {
            response.sendRedirect("/login?error=true");
        }
    }
}
