package org.link.linkvault.config;

import lombok.RequiredArgsConstructor;
import org.link.linkvault.dto.FavoriteBookmarkResponseDto;
import org.link.linkvault.dto.MenuItemResponseDto;
import org.link.linkvault.entity.MenuType;
import org.link.linkvault.entity.Role;
import org.link.linkvault.entity.User;
import org.link.linkvault.service.*;;
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
    private final SavedBookmarkService savedBookmarkService;
    private final FavoriteBookmarkService favoriteBookmarkService;
    private final NotificationService notificationService;
    private final UserService userService;
    private final AnnouncementService announcementService;
    private final QnaArticleService qnaArticleService;
    private final UserSettingsService userSettingsService;

    @ModelAttribute("sidebarMenuItems")
    public List<MenuItemResponseDto> sidebarMenuItems() {
        return getFilteredMenu(MenuType.SIDEBAR);
    }

    @ModelAttribute("adminMenuItems")
    public List<MenuItemResponseDto> adminMenuItems() {
        return getFilteredMenu(MenuType.ADMIN_SIDEBAR);
    }

    @ModelAttribute("savedCount")
    public long savedCount() {
        User user = getCurrentUser();
        if (user == null) return 0;
        return savedBookmarkService.getCount(user);
    }

    @ModelAttribute("favorites")
    public List<FavoriteBookmarkResponseDto> favorites() {
        User user = getCurrentUser();
        if (user == null) return Collections.emptyList();
        return favoriteBookmarkService.getFavorites(user);
    }

    @ModelAttribute("unreadNotificationCount")
    public long unreadNotificationCount() {
        User user = getCurrentUser();
        if (user == null) return 0;
        return notificationService.getUnreadCount(user);
    }

    @ModelAttribute("unreadAnnouncementCount")
    public long unreadAnnouncementCount() {
        User user = getCurrentUser();
        if (user == null) return 0;
        return announcementService.getUnreadCountForUser(user);
    }

    @ModelAttribute("qnaHasRecentUpdate")
    public boolean qnaHasRecentUpdate() {
        return qnaArticleService.hasRecentlyUpdatedArticles();
    }

    @ModelAttribute("currentTheme")
    public String currentTheme() {
        User user = getCurrentUser();
        if (user == null) return "DARK";
        return userSettingsService.getThemeForUser(user.getId()).name();
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }
        try {
            return userService.getUserEntity(auth.getName());
        } catch (Exception e) {
            return null;
        }
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
        return Role.MEMBER;
    }
}
