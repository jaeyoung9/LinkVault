package org.link.linkvault.service;

import lombok.RequiredArgsConstructor;
import org.link.linkvault.dto.PrivacyPolicyResponseDto;
import org.link.linkvault.dto.RegisterRequestDto;
import org.link.linkvault.entity.InvitationCode;
import org.link.linkvault.entity.Role;
import org.link.linkvault.entity.User;
import org.link.linkvault.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final InvitationService invitationService;
    private final PrivacyPolicyService privacyPolicyService;

    @Transactional
    public User registerWithInvitation(RegisterRequestDto dto) {
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

        // Consume invitation (with pessimistic lock + re-check)
        invitationService.consume(invitation, user);

        return user;
    }
}
