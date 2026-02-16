package org.link.linkvault.config;

import lombok.RequiredArgsConstructor;
import org.link.linkvault.dto.MenuItemResponseDto;
import org.link.linkvault.entity.MenuType;
import org.link.linkvault.entity.Role;
import org.link.linkvault.service.MenuService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

@ControllerAdvice
@RequiredArgsConstructor
public class MenuModelAdvice {

    private final MenuService menuService;

    @ModelAttribute("sidebarMenuItems")
    public List<MenuItemResponseDto> sidebarMenuItems() {
        return getFilteredMenu(MenuType.SIDEBAR);
    }

    @ModelAttribute("adminMenuItems")
    public List<MenuItemResponseDto> adminMenuItems() {
        return getFilteredMenu(MenuType.ADMIN_SIDEBAR);
    }

    private List<MenuItemResponseDto> getFilteredMenu(MenuType menuType) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return Collections.emptyList();
        }

        List<MenuItemResponseDto> items = menuService.getMenuItems(menuType);
        Role userRole = extractRole(auth.getAuthorities());
        return menuService.filterByAccess(items, userRole, auth.getAuthorities());
    }

    private Role extractRole(Collection<? extends GrantedAuthority> authorities) {
        for (GrantedAuthority authority : authorities) {
            String auth = authority.getAuthority();
            if (auth.startsWith("ROLE_")) {
                try {
                    return Role.valueOf(auth.substring(5));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
        return Role.USER;
    }
}
