package org.link.linkvault.service;

import lombok.RequiredArgsConstructor;
import org.link.linkvault.dto.PrivacyPolicyResponseDto;
import org.link.linkvault.entity.PrivacyPolicy;
import org.link.linkvault.entity.User;
import org.link.linkvault.repository.PrivacyPolicyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PrivacyPolicyService {

    private final PrivacyPolicyRepository privacyPolicyRepository;

    public PrivacyPolicyResponseDto getActivePolicy() {
        return privacyPolicyRepository.findByActiveTrue()
                .map(PrivacyPolicyResponseDto::from)
                .orElse(null);
    }

    @Transactional
    public PrivacyPolicyResponseDto update(String content, User admin) {
        int nextVersion = 1;
        PrivacyPolicy current = privacyPolicyRepository.findByActiveTrue().orElse(null);
        if (current != null) {
            nextVersion = current.getVersion() + 1;
            current.deactivate();
        }

        PrivacyPolicy newPolicy = PrivacyPolicy.builder()
                .content(content)
                .version(nextVersion)
                .active(true)
                .updatedBy(admin)
                .build();

        return PrivacyPolicyResponseDto.from(privacyPolicyRepository.save(newPolicy));
    }

    public List<PrivacyPolicyResponseDto> findAll() {
        return privacyPolicyRepository.findAllByOrderByVersionDesc().stream()
                .map(PrivacyPolicyResponseDto::from)
                .collect(Collectors.toList());
    }
}
