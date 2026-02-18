package org.link.linkvault.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    // Per-IP bucket cache
    private final Map<String, Bucket> loginBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> registerBuckets = new ConcurrentHashMap<>();

    // Login: 10 attempts per minute per IP
    private Bucket createLoginBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.classic(10, Refill.greedy(10, Duration.ofMinutes(1))))
                .build();
    }

    // Register: 5 attempts per minute per IP
    private Bucket createRegisterBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.classic(5, Refill.greedy(5, Duration.ofMinutes(1))))
                .build();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        String method = request.getMethod();

        // Only rate-limit POST to /login and /api/auth/register
        if ("POST".equalsIgnoreCase(method)) {
            String clientIp = getClientIp(request);

            if ("/login".equals(path)) {
                Bucket bucket = loginBuckets.computeIfAbsent(clientIp, k -> createLoginBucket());
                if (!bucket.tryConsume(1)) {
                    response.setStatus(429);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"message\":\"Too many login attempts. Please try again later.\"}");
                    return;
                }
            } else if ("/api/auth/register".equals(path)) {
                Bucket bucket = registerBuckets.computeIfAbsent(clientIp, k -> createRegisterBucket());
                if (!bucket.tryConsume(1)) {
                    response.setStatus(429);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"message\":\"Too many registration attempts. Please try again later.\"}");
                    return;
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    private String getClientIp(HttpServletRequest request) {
        // Support reverse proxy X-Forwarded-For (production LB)
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }
}
