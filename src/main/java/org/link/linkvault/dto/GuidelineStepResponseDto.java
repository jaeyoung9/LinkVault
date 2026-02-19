package org.link.linkvault.dto;

import lombok.Builder;
import lombok.Getter;
import org.link.linkvault.entity.GuidelineStep;

import java.time.LocalDateTime;

@Getter
@Builder
public class GuidelineStepResponseDto {

    private Long id;
    private String screen;
    private String targetElement;
    private String title;
    private String content;
    private int displayOrder;
    private boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static GuidelineStepResponseDto from(GuidelineStep step) {
        return GuidelineStepResponseDto.builder()
                .id(step.getId())
                .screen(step.getScreen())
                .targetElement(step.getTargetElement())
                .title(step.getTitle())
                .content(step.getContent())
                .displayOrder(step.getDisplayOrder())
                .enabled(step.isEnabled())
                .createdAt(step.getCreatedAt())
                .updatedAt(step.getUpdatedAt())
                .build();
    }
}
