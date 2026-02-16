package org.link.linkvault.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.link.linkvault.entity.MenuType;
import org.link.linkvault.entity.Role;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MenuItemRequestDto {

    @NotBlank(message = "Label is required")
    private String label;

    @NotBlank(message = "URL is required")
    private String url;

    private String icon;

    @NotNull(message = "Menu type is required")
    private MenuType menuType;

    private Long parentId;

    private Integer displayOrder;

    private Role requiredRole;

    private String requiredPermission;
}
