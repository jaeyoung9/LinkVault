package org.link.linkvault.controller;

import lombok.RequiredArgsConstructor;
import org.link.linkvault.dto.RegisterRequestDto;
import org.link.linkvault.entity.InvitationCode;
import org.link.linkvault.entity.User;
import org.link.linkvault.dto.PrivacyPolicyResponseDto;
import org.link.linkvault.repository.UserRepository;
import org.link.linkvault.service.AuthService;
import org.link.linkvault.service.InvitationService;
import org.link.linkvault.service.PrivacyPolicyService;
import org.springframework.http.ResponseEntity;
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
    private final AuthService authService;
    private final InvitationService invitationService;
    private final PrivacyPolicyService privacyPolicyService;

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@Valid @RequestBody RegisterRequestDto dto) {
        authService.registerWithInvitation(dto);
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
        if (userDetails == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Authentication required"));
        }

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
