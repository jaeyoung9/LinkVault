package org.link.linkvault.dto;

import lombok.Builder;
import lombok.Getter;
import org.link.linkvault.entity.Permission;

@Getter
@Builder
public class PermissionResponseDto {

    private Long id;
    private String name;
    private String description;
    private String category;
    private boolean granted;

    public static PermissionResponseDto from(Permission permission, boolean granted) {
        return PermissionResponseDto.builder()
                .id(permission.getId())
                .name(permission.getName())
                .description(permission.getDescription())
                .category(permission.getCategory())
                .granted(granted)
                .build();
    }
}
