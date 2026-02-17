package org.link.linkvault.dto;

import lombok.Builder;
import lombok.Getter;
import org.link.linkvault.entity.SystemSettings;

import java.time.LocalDateTime;

@Getter
@Builder
public class SystemSettingsResponseDto {

    private Long id;
    private String settingKey;
    private String settingValue;
    private String description;
    private String category;
    private LocalDateTime updatedAt;

    public static SystemSettingsResponseDto from(SystemSettings entity) {
        return SystemSettingsResponseDto.builder()
                .id(entity.getId())
                .settingKey(entity.getSettingKey())
                .settingValue(entity.getSettingValue())
                .description(entity.getDescription())
                .category(entity.getCategory())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
