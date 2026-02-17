package org.link.linkvault.controller;

import lombok.RequiredArgsConstructor;
import org.link.linkvault.dto.RegisterRequestDto;
import org.link.linkvault.entity.InvitationCode;
import org.link.linkvault.entity.Role;
import org.link.linkvault.entity.User;
import org.link.linkvault.dto.PrivacyPolicyResponseDto;
import org.link.linkvault.repository.UserRepository;
import org.link.linkvault.service.InvitationService;
import org.link.linkvault.service.PrivacyPolicyService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;

import javax.validation.Valid;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final InvitationService invitationService;
    private final PrivacyPolicyService privacyPolicyService;

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@Valid @RequestBody RegisterRequestDto dto) {
        // Validate invitation code
        InvitationCode invitation = invitationService.validate(dto.getInvitationCode());

        // Check username/email uniqueness
        if (userRepository.existsByUsername(dto.getUsername())) {
            throw new IllegalArgumentException("Username already exists: " + dto.getUsername());
        }
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("Email already exists: " + dto.getEmail());
        }

        // Determine role from invitation code
        Role role = invitation.getAssignedRole() != null ? invitation.getAssignedRole() : Role.MEMBER;

        // Create user
        User user = User.builder()
                .username(dto.getUsername())
                .email(dto.getEmail())
                .password(passwordEncoder.encode(dto.getPassword()))
                .role(role)
                .enabled(true)
                .build();
        // Record privacy policy agreement
        PrivacyPolicyResponseDto activePolicy = privacyPolicyService.getActivePolicy();
        if (activePolicy != null) {
            user.agreeToPrivacyPolicy(activePolicy.getVersion());
        }

        user = userRepository.save(user);

        // Record invitation use
        invitationService.consume(invitation, user);

        return ResponseEntity.ok(Map.of("message", "Registration successful. You can now log in."));
    }

    @GetMapping("/validate-code")
    public ResponseEntity<Map<String, Object>> validateCode(@RequestParam String code) {
        try {
            InvitationCode invitation = invitationService.validate(code);
            return ResponseEntity.ok(Map.of(
                    "valid", true,
                    "assignedRole", invitation.getAssignedRole() != null ? invitation.getAssignedRole().name() : "MEMBER"
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(Map.of("valid", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/privacy-consent")
    public ResponseEntity<Map<String, String>> privacyConsent(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody Map<String, Boolean> body) {
        boolean agree = Boolean.TRUE.equals(body.get("agree"));

        if (agree) {
            PrivacyPolicyResponseDto activePolicy = privacyPolicyService.getActivePolicy();
            if (activePolicy == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "No active privacy policy found"));
            }

            User user = userRepository.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            user.agreeToPrivacyPolicy(activePolicy.getVersion());
            userRepository.save(user);

            return ResponseEntity.ok(Map.of("message", "Privacy policy consent recorded"));
        }

        // Decline: client handles logout via form submit
        return ResponseEntity.ok(Map.of("message", "Consent declined"));
    }

    @GetMapping("/privacy-policy")
    public ResponseEntity<?> getPrivacyPolicy() {
        PrivacyPolicyResponseDto policy = privacyPolicyService.getActivePolicy();
        if (policy == null) {
            return ResponseEntity.ok(Map.of("content", "", "version", 0));
        }
        return ResponseEntity.ok(Map.of("content", policy.getContent(), "version", policy.getVersion()));
    }
}
