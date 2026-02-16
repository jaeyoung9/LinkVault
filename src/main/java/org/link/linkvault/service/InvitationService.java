package org.link.linkvault.service;

import lombok.RequiredArgsConstructor;
import org.link.linkvault.dto.InvitationCodeRequestDto;
import org.link.linkvault.dto.InvitationCodeResponseDto;
import org.link.linkvault.entity.InvitationCode;
import org.link.linkvault.entity.InvitationUse;
import org.link.linkvault.entity.Role;
import org.link.linkvault.entity.User;
import org.link.linkvault.exception.ResourceNotFoundException;
import org.link.linkvault.repository.InvitationCodeRepository;
import org.link.linkvault.repository.InvitationUseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InvitationService {

    private final InvitationCodeRepository invitationCodeRepository;
    private final InvitationUseRepository invitationUseRepository;

    public List<InvitationCodeResponseDto> findAll() {
        return invitationCodeRepository.findAllWithCreator().stream()
                .map(InvitationCodeResponseDto::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public InvitationCodeResponseDto create(InvitationCodeRequestDto dto, User createdBy) {
        String code = UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        LocalDateTime expiresAt = null;
        if (dto.getExpiresInHours() != null && dto.getExpiresInHours() > 0) {
            expiresAt = LocalDateTime.now().plusHours(dto.getExpiresInHours());
        }

        InvitationCode invitation = InvitationCode.builder()
                .code(code)
                .createdBy(createdBy)
                .maxUses(dto.getMaxUses() > 0 ? dto.getMaxUses() : 1)
                .active(true)
                .expiresAt(expiresAt)
                .note(dto.getNote())
                .assignedRole(dto.getAssignedRole() != null ? dto.getAssignedRole() : Role.USER)
                .build();

        invitation = invitationCodeRepository.save(invitation);
        return InvitationCodeResponseDto.from(invitation);
    }

    public InvitationCode validate(String code) {
        InvitationCode invitation = invitationCodeRepository.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("Invalid invitation code"));

        if (!invitation.isUsable()) {
            throw new IllegalArgumentException("Invitation code is no longer valid");
        }

        return invitation;
    }

    @Transactional
    public void consume(InvitationCode invitation, User user) {
        // Re-fetch to get a managed entity (invitation is detached from validate()'s read-only tx)
        InvitationCode managed = invitationCodeRepository.findById(invitation.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Invitation code not found"));
        managed.recordUse();
        invitationUseRepository.save(new InvitationUse(managed, user));
    }

    @Transactional
    public void toggleActive(Long id) {
        InvitationCode invitation = invitationCodeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invitation code not found: " + id));
        invitation.setActive(!invitation.isActive());
    }

    @Transactional
    public void delete(Long id) {
        InvitationCode invitation = invitationCodeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invitation code not found: " + id));
        invitationCodeRepository.delete(invitation);
    }
}
