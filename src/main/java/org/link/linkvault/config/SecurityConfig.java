package org.link.linkvault.config;

import lombok.RequiredArgsConstructor;
import org.link.linkvault.service.CustomUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final PrivacyConsentSuccessHandler privacyConsentSuccessHandler;
    private final PrivacyConsentFilter privacyConsentFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authenticationProvider(authenticationProvider())
            .authorizeRequests()
                .antMatchers("/css/**", "/js/**", "/files/**", "/login", "/register", "/error").permitAll()
                .antMatchers("/api/auth/register", "/api/auth/validate-code", "/api/auth/privacy-policy").permitAll()
                // Guest access: read-only pages
                .antMatchers(HttpMethod.GET, "/", "/map", "/bookmark/**", "/search", "/tag/**",
                        "/qna", "/qna/**", "/announcements", "/announcements/**",
                        "/transparency", "/transparency/**",
                        "/policies/**").permitAll()
                // Guest search API
                .antMatchers(HttpMethod.GET, "/api/search").permitAll()
                // Guest event tracking + ad feedback
                .antMatchers(HttpMethod.POST, "/api/guest/event").permitAll()
                .antMatchers(HttpMethod.POST, "/api/ad/hide").permitAll()
                // Stripe webhook (no auth, verified by signature)
                .antMatchers(HttpMethod.POST, "/api/stripe/webhook").permitAll()
                .antMatchers("/api/auth/privacy-consent").authenticated()
                .antMatchers("/admin/**", "/api/admin/**").hasAnyRole("SUPER_ADMIN", "COMMUNITY_ADMIN", "MODERATOR")
                .antMatchers("/h2-console/**").hasAnyRole("SUPER_ADMIN", "COMMUNITY_ADMIN")
                .anyRequest().authenticated()
            .and()
            .formLogin()
                .loginPage("/login")
                .successHandler(privacyConsentSuccessHandler)
                .permitAll()
            .and()
            .logout()
                .logoutSuccessUrl("/login?logout=true")
                .permitAll()
            .and()
            .csrf()
                .ignoringAntMatchers("/api/auth/register", "/api/auth/validate-code",
                                     "/api/auth/privacy-policy", "/h2-console/**",
                                     "/api/guest/**", "/api/ad/**", "/api/stripe/**")
            .and()
            .headers()
                .frameOptions().sameOrigin();

        http.addFilterAfter(privacyConsentFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
