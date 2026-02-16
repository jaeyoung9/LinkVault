package org.link.linkvault.controller;

import lombok.RequiredArgsConstructor;
import org.link.linkvault.dto.SystemStatsDto;
import org.link.linkvault.entity.User;
import org.link.linkvault.service.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminWebController {

    private final UserService userService;
    private final BookmarkService bookmarkService;
    private final TagService tagService;
    private final AuditLogService auditLogService;
    private final SystemStatsService systemStatsService;
    private final DatabaseBackupService databaseBackupService;
    private final QnaArticleService qnaArticleService;
    private final AnnouncementService announcementService;

    @GetMapping
    @PreAuthorize("hasAuthority('VIEW_STATS')")
    public String dashboard(Model model) {
        SystemStatsDto stats = systemStatsService.getSystemStats();
        model.addAttribute("stats", stats);
        return "admin/dashboard";
    }

    @GetMapping("/users")
    @PreAuthorize("hasAuthority('MANAGE_USERS')")
    public String users(Model model) {
        model.addAttribute("users", userService.findAll());
        return "admin/users";
    }

    @GetMapping("/bookmarks")
    @PreAuthorize("hasAuthority('MANAGE_BOOKMARKS')")
    public String bookmarks(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Model model) {
        User currentUser = userService.getUserEntity(userDetails.getUsername());
        model.addAttribute("bookmarks", bookmarkService.findAll(currentUser,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))));
        return "admin/bookmarks";
    }

    @GetMapping("/tags")
    @PreAuthorize("hasAuthority('MANAGE_TAGS')")
    public String tags(Model model) {
        model.addAttribute("tags", tagService.findAll());
        return "admin/tags";
    }

    @GetMapping("/audit")
    @PreAuthorize("hasAuthority('VIEW_AUDIT_LOG')")
    public String audit(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Model model) {
        model.addAttribute("logs", auditLogService.findAll(PageRequest.of(page, size)));
        return "admin/audit";
    }

    @GetMapping("/backup")
    @PreAuthorize("hasAuthority('MANAGE_BACKUP')")
    public String backup(Model model) {
        model.addAttribute("backups", databaseBackupService.listBackups());
        return "admin/backup";
    }

    @GetMapping("/comments")
    @PreAuthorize("hasAuthority('MODERATE_COMMENTS')")
    public String comments() {
        return "admin/comments";
    }

    @GetMapping("/invitations")
    @PreAuthorize("hasAuthority('MANAGE_INVITATIONS')")
    public String invitations() {
        return "admin/invitations";
    }

    @GetMapping("/menus")
    @PreAuthorize("hasAuthority('MANAGE_MENUS')")
    public String menus() {
        return "admin/menus";
    }

    @GetMapping("/permissions")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public String permissions() {
        return "admin/permissions";
    }

    @GetMapping("/qna")
    @PreAuthorize("hasAuthority('MANAGE_QNA')")
    public String qnaManagement(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Model model) {
        model.addAttribute("articles", qnaArticleService.findAllAdmin(PageRequest.of(page, size)));
        return "admin/qna";
    }

    @GetMapping("/announcements")
    @PreAuthorize("hasAuthority('MANAGE_ANNOUNCEMENTS')")
    public String announcementsManagement(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Model model) {
        model.addAttribute("announcements", announcementService.findAllAdmin(PageRequest.of(page, size)));
        return "admin/announcements";
    }
}
