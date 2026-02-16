package org.link.linkvault.service;

import lombok.RequiredArgsConstructor;
import org.link.linkvault.dto.MenuItemOrderDto;
import org.link.linkvault.dto.MenuItemRequestDto;
import org.link.linkvault.dto.MenuItemResponseDto;
import org.link.linkvault.entity.MenuItem;
import org.link.linkvault.entity.MenuType;
import org.link.linkvault.entity.Role;
import org.link.linkvault.exception.ResourceNotFoundException;
import org.link.linkvault.repository.MenuItemRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MenuService {

    private final MenuItemRepository menuItemRepository;
    private final ConcurrentHashMap<String, List<MenuItemResponseDto>> menuCache = new ConcurrentHashMap<>();

    public List<MenuItemResponseDto> getMenuItems(MenuType menuType) {
        return menuCache.computeIfAbsent(menuType.name(), key ->
                menuItemRepository.findVisibleRootsByMenuType(menuType).stream()
                        .map(MenuItemResponseDto::from)
                        .collect(Collectors.toList()));
    }

    public List<MenuItemResponseDto> getAllMenuItems(MenuType menuType) {
        return menuItemRepository.findRootsByMenuType(menuType).stream()
                .map(MenuItemResponseDto::from)
                .collect(Collectors.toList());
    }

    public List<MenuItemResponseDto> filterByAccess(List<MenuItemResponseDto> items,
                                                     Role userRole,
                                                     Collection<? extends GrantedAuthority> authorities) {
        return items.stream()
                .filter(item -> hasAccess(item, userRole, authorities))
                .map(item -> filterChildren(item, userRole, authorities))
                .collect(Collectors.toList());
    }

    private boolean hasAccess(MenuItemResponseDto item, Role userRole,
                              Collection<? extends GrantedAuthority> authorities) {
        if (userRole == Role.SUPER_ADMIN) return true;

        if (item.getRequiredRole() != null) {
            int userLevel = getRoleLevel(userRole);
            int requiredLevel = getRoleLevel(item.getRequiredRole());
            if (userLevel < requiredLevel) return false;
        }

        if (item.getRequiredPermission() != null && !item.getRequiredPermission().isEmpty()) {
            boolean hasPermission = authorities.stream()
                    .anyMatch(a -> a.getAuthority().equals(item.getRequiredPermission()));
            if (!hasPermission) return false;
        }

        return true;
    }

    private MenuItemResponseDto filterChildren(MenuItemResponseDto item, Role userRole,
                                                Collection<? extends GrantedAuthority> authorities) {
        if (item.getChildren() == null || item.getChildren().isEmpty()) return item;

        List<MenuItemResponseDto> filteredChildren = item.getChildren().stream()
                .filter(child -> hasAccess(child, userRole, authorities))
                .map(child -> filterChildren(child, userRole, authorities))
                .collect(Collectors.toList());

        return MenuItemResponseDto.builder()
                .id(item.getId())
                .label(item.getLabel())
                .url(item.getUrl())
                .icon(item.getIcon())
                .menuType(item.getMenuType())
                .parentId(item.getParentId())
                .displayOrder(item.getDisplayOrder())
                .visible(item.isVisible())
                .requiredRole(item.getRequiredRole())
                .requiredPermission(item.getRequiredPermission())
                .systemItem(item.isSystemItem())
                .children(filteredChildren)
                .build();
    }

    private int getRoleLevel(Role role) {
        switch (role) {
            case SUPER_ADMIN: return 4;
            case COMMUNITY_ADMIN: return 3;
            case MODERATOR: return 2;
            case MEMBER: return 1;
            default: return 0;
        }
    }

    @Transactional
    public MenuItemResponseDto create(MenuItemRequestDto dto, String createdBy) {
        MenuItem parent = null;
        if (dto.getParentId() != null) {
            parent = menuItemRepository.findById(dto.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent menu item not found"));
        }

        MenuItem item = MenuItem.builder()
                .label(dto.getLabel())
                .url(dto.getUrl())
                .icon(dto.getIcon())
                .menuType(dto.getMenuType())
                .parent(parent)
                .displayOrder(dto.getDisplayOrder() != null ? dto.getDisplayOrder() : 0)
                .visible(true)
                .requiredRole(dto.getRequiredRole())
                .requiredPermission(dto.getRequiredPermission())
                .systemItem(false)
                .createdBy(createdBy)
                .build();

        item = menuItemRepository.save(item);
        clearCache();
        return MenuItemResponseDto.from(item);
    }

    @Transactional
    public MenuItemResponseDto update(Long id, MenuItemRequestDto dto) {
        MenuItem item = menuItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Menu item not found: " + id));

        item.update(dto.getLabel(), dto.getUrl(), dto.getIcon(),
                dto.getRequiredRole(), dto.getRequiredPermission());

        if (dto.getDisplayOrder() != null) {
            item.setDisplayOrder(dto.getDisplayOrder());
        }

        clearCache();
        return MenuItemResponseDto.from(item);
    }

    @Transactional
    public void delete(Long id) {
        MenuItem item = menuItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Menu item not found: " + id));
        menuItemRepository.delete(item);
        clearCache();
    }

    @Transactional
    public void toggleVisibility(Long id) {
        MenuItem item = menuItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Menu item not found: " + id));
        item.setVisible(!item.isVisible());
        clearCache();
    }

    @Transactional
    public void reorder(List<MenuItemOrderDto> orders) {
        for (MenuItemOrderDto order : orders) {
            MenuItem item = menuItemRepository.findById(order.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Menu item not found: " + order.getId()));
            item.setDisplayOrder(order.getDisplayOrder());
            if (order.getParentId() != null) {
                MenuItem parent = menuItemRepository.findById(order.getParentId())
                        .orElseThrow(() -> new ResourceNotFoundException("Parent not found: " + order.getParentId()));
                item.setParent(parent);
            } else {
                item.setParent(null);
            }
        }
        clearCache();
    }

    public void clearCache() {
        menuCache.clear();
    }
}
