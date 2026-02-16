package org.link.linkvault.dto;

import lombok.Builder;
import lombok.Getter;
import org.link.linkvault.entity.MenuItem;
import org.link.linkvault.entity.MenuType;
import org.link.linkvault.entity.Role;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
public class MenuItemResponseDto {

    private Long id;
    private String label;
    private String url;
    private String icon;
    private MenuType menuType;
    private Long parentId;
    private int displayOrder;
    private boolean visible;
    private Role requiredRole;
    private String requiredPermission;
    private boolean systemItem;
    private List<MenuItemResponseDto> children;

    public static MenuItemResponseDto from(MenuItem item) {
        return MenuItemResponseDto.builder()
                .id(item.getId())
                .label(item.getLabel())
                .url(item.getUrl())
                .icon(item.getIcon())
                .menuType(item.getMenuType())
                .parentId(item.getParent() != null ? item.getParent().getId() : null)
                .displayOrder(item.getDisplayOrder())
                .visible(item.isVisible())
                .requiredRole(item.getRequiredRole())
                .requiredPermission(item.getRequiredPermission())
                .systemItem(item.isSystemItem())
                .children(item.getChildren().stream()
                        .map(MenuItemResponseDto::from)
                        .collect(Collectors.toList()))
                .build();
    }
}
