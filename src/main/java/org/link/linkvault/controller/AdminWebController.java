package org.link.linkvault.controller;

import lombok.RequiredArgsConstructor;
import org.link.linkvault.dto.SystemStatsDto;
import org.link.linkvault.entity.User;
import org.link.linkvault.service.*;
import org.link.linkvault.service.ReportService;
import org.springframework.core.env.Environment;
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

import java.time.LocalDateTime;

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
    private final PrivacyPolicyService privacyPolicyService;
    private final ReportService reportService;
    private final MonetizationStatsService monetizationStatsService;
    private final GuestEventService guestEventService;
    private final TransparencyReportService transparencyReportService;
    private final Environment env;

    @GetMapping
    @PreAuthorize("hasAuthority('VIEW_STATS')")
    public String dashboard(Model model) {
        SystemStatsDto stats = systemStatsService.getSystemStats();
        model.addAttribute("stats", stats);
        return "admin/dashboard";
    }

    @GetMapping("/users")
    @PreAuthorize("hasAuthority('USER_MANAGE')")
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
            @RequestParam(required = false) Boolean deleted,
            Model model) {
        if (Boolean.TRUE.equals(deleted)) {
            model.addAttribute("bookmarks", bookmarkService.findAllDeleted(
                    PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"))));
            model.addAttribute("showDeleted", true);
        } else {
            model.addAttribute("bookmarks", bookmarkService.findAllForAdmin(
                    PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))));
            model.addAttribute("showDeleted", false);
        }
        return "admin/bookmarks";
    }

    @GetMapping("/tags")
    @PreAuthorize("hasAuthority('MANAGE_TAGS')")
    public String tags(Model model) {
        model.addAttribute("tags", tagService.findAll());
        return "admin/tags";
    }

    @GetMapping("/audit")
    @PreAuthorize("hasAuthority('AUDIT_VIEW')")
    public String audit(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Model model) {
        model.addAttribute("logs", auditLogService.findAll(PageRequest.of(page, size)));
        return "admin/audit";
    }

    @GetMapping("/backup")
    @PreAuthorize("hasAuthority('BACKUP_RUN')")
    public String backup(Model model) {
        model.addAttribute("backups", databaseBackupService.listBackups());
        return "admin/backup";
    }

    @GetMapping("/comments")
    @PreAuthorize("hasAuthority('COMMENT_HIDE')")
    public String comments() {
        return "admin/comments";
    }

    @GetMapping("/invitations")
    @PreAuthorize("hasAuthority('INVITE_ISSUE')")
    public String invitations() {
        return "admin/invitations";
    }

    @GetMapping("/menus")
    @PreAuthorize("hasAuthority('MENU_MANAGE')")
    public String menus() {
        return "admin/menus";
    }

    @GetMapping("/permissions")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public String permissions() {
        return "admin/permissions";
    }

    @GetMapping("/reports")
    @PreAuthorize("hasAuthority('REPORT_REVIEW')")
    public String reports(Model model) {
        model.addAttribute("pendingCount", reportService.getPendingCount());
        return "admin/reports";
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

    @GetMapping("/privacy-policy")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public String privacyPolicy(Model model) {
        model.addAttribute("activePolicy", privacyPolicyService.getActivePolicy());
        model.addAttribute("history", privacyPolicyService.findAll());
        return "admin/privacy-policy";
    }

    @GetMapping("/guidelines")
    @PreAuthorize("hasAuthority('SYSTEM_SETTINGS')")
    public String guidelines() {
        return "admin/guidelines";
    }

    @GetMapping("/settings")
    @PreAuthorize("hasAuthority('SYSTEM_SETTINGS')")
    public String settings(Model model) {
        model.addAttribute("dbInfo", env.getProperty("spring.datasource.url", "unknown"));
        return "admin/settings";
    }

    @GetMapping("/monetization")
    @PreAuthorize("hasAuthority('VIEW_MONETIZATION')")
    public String monetization(Model model) {
        model.addAttribute("stats", monetizationStatsService.getStats());
        return "admin/monetization";
    }

    @GetMapping("/guest-analytics")
    @PreAuthorize("hasAuthority('VIEW_MONETIZATION')")
    public String guestAnalytics(
            @RequestParam(defaultValue = "90") int days,
            Model model) {
        LocalDateTime from = LocalDateTime.now().minusDays(days);
        LocalDateTime to = LocalDateTime.now();
        model.addAttribute("funnel", guestEventService.getFunnelStats(from, to));
        model.addAttribute("days", days);
        return "admin/guest-analytics";
    }

    @GetMapping("/transparency-reports")
    @PreAuthorize("hasAuthority('VIEW_MONETIZATION')")
    public String transparencyReports(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Model model) {
        model.addAttribute("reports", transparencyReportService.findAll(
                PageRequest.of(page, size)));
        return "admin/transparency-reports";
    }
}
