package org.link.linkvault.dto;

import lombok.Builder;
import lombok.Getter;
import org.link.linkvault.entity.PrivacyPolicy;

import java.time.LocalDateTime;

@Getter
@Builder
public class PrivacyPolicyResponseDto {

    private Long id;
    private String content;
    private int version;
    private boolean active;
    private String updatedByUsername;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static PrivacyPolicyResponseDto from(PrivacyPolicy policy) {
        return PrivacyPolicyResponseDto.builder()
                .id(policy.getId())
                .content(policy.getContent())
                .version(policy.getVersion())
                .active(policy.isActive())
                .updatedByUsername(policy.getUpdatedBy() != null ? policy.getUpdatedBy().getUsername() : null)
                .createdAt(policy.getCreatedAt())
                .updatedAt(policy.getUpdatedAt())
                .build();
    }
}
