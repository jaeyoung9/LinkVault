package org.link.linkvault.controller;

import lombok.RequiredArgsConstructor;
import org.link.linkvault.dto.*;
import org.link.linkvault.entity.ReportStatus;
import org.link.linkvault.entity.Role;
import org.link.linkvault.entity.User;
import org.link.linkvault.service.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Validated
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminApiController {

    private final UserService userService;
    private final BookmarkService bookmarkService;
    private final TagService tagService;
    private final SystemStatsService systemStatsService;
    private final DatabaseBackupService databaseBackupService;
    private final PermissionService permissionService;
    private final MenuService menuService;
    private final InvitationService invitationService;
    private final CommentService commentService;
    private final QnaArticleService qnaArticleService;
    private final AnnouncementService announcementService;
    private final PrivacyPolicyService privacyPolicyService;
    private final ReportService reportService;
    private final SystemSettingsService systemSettingsService;
    private final FileVaultService fileVaultService;
    private final TransparencyReportService transparencyReportService;
    private final MonetizationStatsService monetizationStatsService;
    private final GuestEventService guestEventService;
    private final StripeService stripeService;
    private final org.link.linkvault.repository.AdFreePassRepository adFreePassRepository;
    private final AccountLockoutService accountLockoutService;

    // --- User CRUD ---

    @PostMapping("/users")
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    public ResponseEntity<UserResponseDto> createUser(
            @Valid @RequestBody UserRequestDto dto,
            @AuthenticationPrincipal UserDetails userDetails) {
        UserResponseDto result = userService.create(dto, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @PutMapping("/users/{id}")
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    public ResponseEntity<UserResponseDto> updateUser(
            @PathVariable Long id, @Valid @RequestBody UserRequestDto dto,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(userService.update(id, dto, userDetails.getUsername()));
    }

    @DeleteMapping("/users/{id}")
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    public ResponseEntity<Void> deleteUser(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        userService.delete(id, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/users/{id}/toggle")
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    public ResponseEntity<UserResponseDto> toggleUser(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(userService.toggleEnabled(id, userDetails.getUsername()));
    }

    @PostMapping("/users/bulk-deactivate-non-consented")
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    public ResponseEntity<Map<String, Object>> bulkDeactivateNonConsented(
            @AuthenticationPrincipal UserDetails userDetails) {
        int count = userService.bulkDeactivateNonConsented(
                "Bulk deactivated by admin: privacy policy not agreed", userDetails.getUsername());
        return ResponseEntity.ok(Map.of("deactivated", count,
                "message", count + " user(s) deactivated for not consenting to privacy policy"));
    }

    @PostMapping("/users/trigger-privacy-deactivation")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> triggerPrivacyDeactivation(
            @AuthenticationPrincipal UserDetails userDetails) {
        int count = userService.bulkDeactivateNonConsented(
                "Auto-deactivated (manual trigger): privacy policy not agreed", userDetails.getUsername());
        return ResponseEntity.ok(Map.of("deactivated", count,
                "message", count + " user(s) deactivated via manual scheduler trigger"));
    }

    // --- Tag Management ---

    @PostMapping("/tags/merge")
    @PreAuthorize("hasAuthority('MANAGE_TAGS')")
    public ResponseEntity<Map<String, String>> mergeTags(
            @RequestBody TagMergeRequestDto dto,
            @AuthenticationPrincipal UserDetails userDetails) {
        tagService.mergeTags(dto.getSourceTagIds(), dto.getTargetTagName(), userDetails.getUsername());
        return ResponseEntity.ok(Map.of("message", "Tags merged successfully"));
    }

    @DeleteMapping("/tags/unused")
    @PreAuthorize("hasAuthority('MANAGE_TAGS')")
    public ResponseEntity<Map<String, Object>> deleteUnusedTags(
            @AuthenticationPrincipal UserDetails userDetails) {
        int count = tagService.deleteUnusedTags(userDetails.getUsername());
        return ResponseEntity.ok(Map.of("deleted", count, "message", count + " unused tags deleted"));
    }

    @DeleteMapping("/tags/{id}")
    @PreAuthorize("hasAuthority('MANAGE_TAGS')")
    public ResponseEntity<Void> deleteTag(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        tagService.delete(id, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    // --- Stats ---

    @GetMapping("/stats")
    @PreAuthorize("hasAuthority('VIEW_STATS')")
    public ResponseEntity<SystemStatsDto> getStats() {
        return ResponseEntity.ok(systemStatsService.getSystemStats());
    }

    // --- Bookmark Management ---

    @DeleteMapping("/bookmarks/{id}")
    @PreAuthorize("hasAuthority('BOOKMARK_DELETE_ANY')")
    public ResponseEntity<Void> deleteBookmark(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        User admin = userService.getUserEntity(userDetails.getUsername());
        bookmarkService.delete(admin, id);
        return ResponseEntity.noContent().build();
    }

    // --- Backup/Restore ---

    @PostMapping("/backup")
    @PreAuthorize("hasAuthority('BACKUP_RUN')")
    public ResponseEntity<Map<String, String>> createBackup(
            @AuthenticationPrincipal UserDetails userDetails) {
        String filename = databaseBackupService.createBackup(userDetails.getUsername());
        return ResponseEntity.ok(Map.of("filename", filename, "message", "Backup created: " + filename));
    }

    @GetMapping("/backups")
    @PreAuthorize("hasAuthority('BACKUP_RUN')")
    public ResponseEntity<java.util.List<String>> listBackups() {
        return ResponseEntity.ok(databaseBackupService.listBackups());
    }

    @PostMapping("/restore")
    @PreAuthorize("hasAuthority('BACKUP_RUN')")
    public ResponseEntity<Map<String, String>> restoreBackup(
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserDetails userDetails) {
        String filename = body.get("filename");
        if (filename == null || filename.isBlank()) {
            throw new IllegalArgumentException("Filename is required");
        }
        databaseBackupService.restoreBackup(filename, userDetails.getUsername());
        return ResponseEntity.ok(Map.of("message", "Database restored from: " + filename));
    }

    // --- Comment Moderation ---

    @GetMapping("/comments")
    @PreAuthorize("hasAuthority('COMMENT_HIDE')")
    public ResponseEntity<List<CommentResponseDto>> getAllComments() {
        return ResponseEntity.ok(commentService.getAllComments());
    }

    @DeleteMapping("/comments/{id}")
    @PreAuthorize("hasAuthority('COMMENT_HIDE')")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        User moderator = userService.getUserEntity(userDetails.getUsername());
        commentService.delete(id, moderator, true);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/comments/{id}/restore")
    @PreAuthorize("hasAuthority('COMMENT_RESTORE')")
    public ResponseEntity<Map<String, String>> restoreComment(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        commentService.restoreComment(id, userDetails.getUsername());
        return ResponseEntity.ok(Map.of("message", "Comment restored"));
    }

    @DeleteMapping("/comments/{id}/purge")
    @PreAuthorize("hasAuthority('CONTENT_PURGE')")
    public ResponseEntity<Void> purgeComment(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        commentService.purgeComment(id, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    // --- Bookmark Soft-Delete Management ---

    @GetMapping("/bookmarks/deleted")
    @PreAuthorize("hasAuthority('BOOKMARK_RESTORE')")
    public ResponseEntity<org.springframework.data.domain.Page<BookmarkResponseDto>> getDeletedBookmarks(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ResponseEntity.ok(bookmarkService.findAllDeleted(
                org.springframework.data.domain.PageRequest.of(page, size,
                        org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "updatedAt"))));
    }

    @PatchMapping("/bookmarks/{id}/restore")
    @PreAuthorize("hasAuthority('BOOKMARK_RESTORE')")
    public ResponseEntity<Map<String, String>> restoreBookmark(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        User admin = userService.getUserEntity(userDetails.getUsername());
        bookmarkService.restore(admin, id);
        return ResponseEntity.ok(Map.of("message", "Bookmark restored"));
    }

    @DeleteMapping("/bookmarks/{id}/purge")
    @PreAuthorize("hasAuthority('CONTENT_PURGE')")
    public ResponseEntity<Void> purgeBookmark(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        User admin = userService.getUserEntity(userDetails.getUsername());
        bookmarkService.purge(admin, id);
        return ResponseEntity.noContent().build();
    }

    // --- Invitation Management ---

    @GetMapping("/invitations")
    @PreAuthorize("hasAuthority('INVITE_ISSUE')")
    public ResponseEntity<List<InvitationCodeResponseDto>> getInvitations() {
        return ResponseEntity.ok(invitationService.findAll());
    }

    @PostMapping("/invitations")
    @PreAuthorize("hasAuthority('INVITE_ISSUE')")
    public ResponseEntity<InvitationCodeResponseDto> createInvitation(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody InvitationCodeRequestDto dto) {
        User creator = userService.getUserEntity(userDetails.getUsername());
        InvitationCodeResponseDto result = invitationService.create(dto, creator, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @PatchMapping("/invitations/{id}/toggle")
    @PreAuthorize("hasAuthority('INVITE_REVOKE')")
    public ResponseEntity<Map<String, String>> toggleInvitation(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        invitationService.toggleActive(id, userDetails.getUsername());
        return ResponseEntity.ok(Map.of("message", "Invitation toggled"));
    }

    @DeleteMapping("/invitations/{id}")
    @PreAuthorize("hasAuthority('INVITE_REVOKE')")
    public ResponseEntity<Void> deleteInvitation(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        invitationService.delete(id, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    // --- Menu Management ---

    @GetMapping("/menus")
    @PreAuthorize("hasAuthority('MENU_MANAGE')")
    public ResponseEntity<List<MenuItemResponseDto>> getMenuItems(
            @RequestParam(defaultValue = "SIDEBAR") org.link.linkvault.entity.MenuType menuType) {
        return ResponseEntity.ok(menuService.getAllMenuItems(menuType));
    }

    @PostMapping("/menus")
    @PreAuthorize("hasAuthority('MENU_MANAGE')")
    public ResponseEntity<MenuItemResponseDto> createMenuItem(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody MenuItemRequestDto dto) {
        MenuItemResponseDto result = menuService.create(dto, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @PutMapping("/menus/{id}")
    @PreAuthorize("hasAuthority('MENU_MANAGE')")
    public ResponseEntity<MenuItemResponseDto> updateMenuItem(
            @PathVariable Long id, @Valid @RequestBody MenuItemRequestDto dto,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(menuService.update(id, dto, userDetails.getUsername()));
    }

    @DeleteMapping("/menus/{id}")
    @PreAuthorize("hasAuthority('MENU_MANAGE')")
    public ResponseEntity<Void> deleteMenuItem(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        menuService.delete(id, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/menus/{id}/toggle")
    @PreAuthorize("hasAuthority('MENU_MANAGE')")
    public ResponseEntity<Map<String, String>> toggleMenuVisibility(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        menuService.toggleVisibility(id, userDetails.getUsername());
        return ResponseEntity.ok(Map.of("message", "Menu visibility toggled"));
    }

    @PatchMapping("/menus/reorder")
    @PreAuthorize("hasAuthority('MENU_MANAGE')")
    public ResponseEntity<Map<String, String>> reorderMenuItems(
            @RequestBody List<MenuItemOrderDto> orders,
            @AuthenticationPrincipal UserDetails userDetails) {
        menuService.reorder(orders, userDetails.getUsername());
        return ResponseEntity.ok(Map.of("message", "Menu reordered"));
    }

    // --- Permission Management ---

    @GetMapping("/permissions/{role}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<PermissionResponseDto>> getPermissionsForRole(@PathVariable Role role) {
        return ResponseEntity.ok(permissionService.getPermissionsForRole(role));
    }

    @PostMapping("/permissions/toggle")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Map<String, String>> togglePermission(
            @Valid @RequestBody RolePermissionRequestDto dto,
            @AuthenticationPrincipal UserDetails userDetails) {
        permissionService.togglePermission(dto.getRole(), dto.getPermissionId(), dto.isGranted(), userDetails.getUsername());
        return ResponseEntity.ok(Map.of("message", "Permission updated"));
    }

    // --- QnA Management ---

    @GetMapping("/qna/{id}")
    @PreAuthorize("hasAuthority('MANAGE_QNA')")
    public ResponseEntity<QnaArticleResponseDto> getQnaArticle(@PathVariable Long id) {
        return ResponseEntity.ok(qnaArticleService.findByIdAdmin(id));
    }

    @PostMapping("/qna")
    @PreAuthorize("hasAuthority('MANAGE_QNA')")
    public ResponseEntity<QnaArticleResponseDto> createQnaArticle(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody QnaArticleRequestDto dto) {
        User creator = userService.getUserEntity(userDetails.getUsername());
        QnaArticleResponseDto result = qnaArticleService.create(dto, creator, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @PutMapping("/qna/{id}")
    @PreAuthorize("hasAuthority('MANAGE_QNA')")
    public ResponseEntity<QnaArticleResponseDto> updateQnaArticle(
            @PathVariable Long id, @Valid @RequestBody QnaArticleRequestDto dto,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(qnaArticleService.update(id, dto, userDetails.getUsername()));
    }

    @PatchMapping("/qna/{id}/status")
    @PreAuthorize("hasAuthority('MANAGE_QNA')")
    public ResponseEntity<Map<String, String>> updateQnaStatus(
            @PathVariable Long id, @Valid @RequestBody QnaStatusUpdateRequestDto dto,
            @AuthenticationPrincipal UserDetails userDetails) {
        qnaArticleService.updateStatus(id, dto.getStatus(), userDetails.getUsername());
        return ResponseEntity.ok(Map.of("message", "Status updated"));
    }

    @DeleteMapping("/qna/{id}")
    @PreAuthorize("hasAuthority('MANAGE_QNA')")
    public ResponseEntity<Void> deleteQnaArticle(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        qnaArticleService.delete(id, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    // --- Announcement Management ---

    @GetMapping("/announcements/{id}")
    @PreAuthorize("hasAuthority('MANAGE_ANNOUNCEMENTS')")
    public ResponseEntity<AnnouncementResponseDto> getAnnouncement(@PathVariable Long id) {
        return ResponseEntity.ok(announcementService.findByIdAdmin(id));
    }

    @PostMapping("/announcements")
    @PreAuthorize("hasAuthority('MANAGE_ANNOUNCEMENTS')")
    public ResponseEntity<AnnouncementResponseDto> createAnnouncement(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody AnnouncementRequestDto dto) {
        User creator = userService.getUserEntity(userDetails.getUsername());
        AnnouncementResponseDto result = announcementService.create(dto, creator, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @PutMapping("/announcements/{id}")
    @PreAuthorize("hasAuthority('MANAGE_ANNOUNCEMENTS')")
    public ResponseEntity<AnnouncementResponseDto> updateAnnouncement(
            @PathVariable Long id, @Valid @RequestBody AnnouncementRequestDto dto,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(announcementService.update(id, dto, userDetails.getUsername()));
    }

    @PatchMapping("/announcements/{id}/status")
    @PreAuthorize("hasAuthority('MANAGE_ANNOUNCEMENTS')")
    public ResponseEntity<Map<String, String>> updateAnnouncementStatus(
            @PathVariable Long id, @Valid @RequestBody AnnouncementStatusUpdateRequestDto dto,
            @AuthenticationPrincipal UserDetails userDetails) {
        announcementService.updateStatus(id, dto.getStatus(), userDetails.getUsername());
        return ResponseEntity.ok(Map.of("message", "Status updated"));
    }

    @DeleteMapping("/announcements/{id}")
    @PreAuthorize("hasAuthority('MANAGE_ANNOUNCEMENTS')")
    public ResponseEntity<Void> deleteAnnouncement(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        announcementService.delete(id, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    // --- Report Moderation ---

    @GetMapping("/reports")
    @PreAuthorize("hasAuthority('REPORT_REVIEW')")
    public ResponseEntity<Page<ReportResponseDto>> getReports(
            @RequestParam(required = false) ReportStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        if (status != null) {
            return ResponseEntity.ok(reportService.findByStatus(status, pageable));
        }
        return ResponseEntity.ok(reportService.findAll(pageable));
    }

    @PatchMapping("/reports/{id}/review")
    @PreAuthorize("hasAuthority('REPORT_REVIEW')")
    public ResponseEntity<ReportResponseDto> reviewReport(
            @PathVariable Long id,
            @Valid @RequestBody ReportStatusUpdateRequestDto dto,
            @AuthenticationPrincipal UserDetails userDetails) {
        User reviewer = userService.getUserEntity(userDetails.getUsername());
        return ResponseEntity.ok(reportService.review(id, reviewer, dto.getStatus()));
    }

    // --- Privacy Policy Management ---

    @GetMapping("/privacy-policy")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<PrivacyPolicyResponseDto> getActivePrivacyPolicy() {
        PrivacyPolicyResponseDto policy = privacyPolicyService.getActivePolicy();
        if (policy == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(policy);
    }

    @PutMapping("/privacy-policy")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<PrivacyPolicyResponseDto> updatePrivacyPolicy(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody Map<String, String> body) {
        User admin = userService.getUserEntity(userDetails.getUsername());
        String content = body.get("content");
        return ResponseEntity.ok(privacyPolicyService.update(content, admin, userDetails.getUsername()));
    }

    @GetMapping("/privacy-policy/history")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<PrivacyPolicyResponseDto>> getPrivacyPolicyHistory() {
        return ResponseEntity.ok(privacyPolicyService.findAll());
    }

    // --- System Settings ---

    @GetMapping("/settings")
    @PreAuthorize("hasAuthority('SYSTEM_SETTINGS')")
    public ResponseEntity<List<SystemSettingsResponseDto>> getAllSettings() {
        return ResponseEntity.ok(systemSettingsService.findAll().stream()
                .map(SystemSettingsResponseDto::from)
                .collect(Collectors.toList()));
    }

    @GetMapping("/settings/category/{category}")
    @PreAuthorize("hasAuthority('SYSTEM_SETTINGS')")
    public ResponseEntity<List<SystemSettingsResponseDto>> getSettingsByCategory(@PathVariable String category) {
        return ResponseEntity.ok(systemSettingsService.findByCategory(category).stream()
                .map(SystemSettingsResponseDto::from)
                .collect(Collectors.toList()));
    }

    @PutMapping("/settings/{key}")
    @PreAuthorize("hasAuthority('SYSTEM_SETTINGS')")
    public ResponseEntity<SystemSettingsResponseDto> updateSetting(
            @PathVariable String key,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserDetails userDetails) {
        String value = body.get("value");
        var updated = systemSettingsService.updateValue(key, value, userDetails.getUsername());
        if (key.startsWith("file-vault.")) {
            fileVaultService.reloadSettings();
        }
        return ResponseEntity.ok(SystemSettingsResponseDto.from(updated));
    }

    // --- Transparency Report Management ---

    @GetMapping("/transparency-reports")
    @PreAuthorize("hasAuthority('VIEW_MONETIZATION')")
    public ResponseEntity<Page<TransparencyReportResponseDto>> getTransparencyReports(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ResponseEntity.ok(transparencyReportService.findAll(PageRequest.of(page, size)));
    }

    @PostMapping("/transparency-reports")
    @PreAuthorize("hasAuthority('VIEW_MONETIZATION')")
    public ResponseEntity<TransparencyReportResponseDto> createTransparencyReport(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody TransparencyReportRequestDto dto) {
        User creator = userService.getUserEntity(userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(transparencyReportService.create(dto, creator));
    }

    @PutMapping("/transparency-reports/{id}")
    @PreAuthorize("hasAuthority('VIEW_MONETIZATION')")
    public ResponseEntity<TransparencyReportResponseDto> updateTransparencyReport(
            @PathVariable Long id,
            @Valid @RequestBody TransparencyReportRequestDto dto) {
        return ResponseEntity.ok(transparencyReportService.update(id, dto));
    }

    @PatchMapping("/transparency-reports/{id}/publish")
    @PreAuthorize("hasAuthority('VIEW_MONETIZATION')")
    public ResponseEntity<Map<String, String>> publishTransparencyReport(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        transparencyReportService.publish(id, userDetails.getUsername());
        return ResponseEntity.ok(Map.of("message", "Report published"));
    }

    @PatchMapping("/transparency-reports/{id}/unpublish")
    @PreAuthorize("hasAuthority('VIEW_MONETIZATION')")
    public ResponseEntity<Map<String, String>> unpublishTransparencyReport(@PathVariable Long id) {
        transparencyReportService.unpublish(id);
        return ResponseEntity.ok(Map.of("message", "Report unpublished"));
    }

    @DeleteMapping("/transparency-reports/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> deleteTransparencyReport(
            @PathVariable Long id) {
        transparencyReportService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // --- Monetization Stats ---

    @GetMapping("/monetization/stats")
    @PreAuthorize("hasAuthority('VIEW_MONETIZATION')")
    public ResponseEntity<MonetizationStatsDto> getMonetizationStats() {
        return ResponseEntity.ok(monetizationStatsService.getStats());
    }

    // --- Guest Funnel Analytics ---

    @GetMapping("/guest-analytics/funnel")
    @PreAuthorize("hasAuthority('VIEW_MONETIZATION')")
    public ResponseEntity<GuestFunnelStatsDto> getGuestFunnel(
            @RequestParam(defaultValue = "90") int days) {
        java.time.LocalDateTime from = java.time.LocalDateTime.now().minusDays(days);
        java.time.LocalDateTime to = java.time.LocalDateTime.now();
        return ResponseEntity.ok(guestEventService.getFunnelStats(from, to));
    }

    // --- Account Lockout Management ---

    @GetMapping("/users/locked")
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    public ResponseEntity<List<Map<String, Object>>> getLockedUsers() {
        List<Map<String, Object>> result = accountLockoutService.getLockedUsers().stream()
                .map(u -> {
                    Map<String, Object> m = new java.util.LinkedHashMap<>();
                    m.put("id", u.getId());
                    m.put("username", u.getUsername());
                    m.put("email", u.getEmail());
                    m.put("failedAttempts", u.getFailedLoginAttempts());
                    m.put("lockedUntil", u.getAccountLockedUntil());
                    return m;
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/users/{id}/unlock")
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    public ResponseEntity<Map<String, String>> unlockUser(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        accountLockoutService.unlockUser(id, userDetails.getUsername());
        return ResponseEntity.ok(Map.of("message", "Account unlocked successfully"));
    }

    // --- Payment Management ---

    @GetMapping("/payments")
    @PreAuthorize("hasAuthority('MANAGE_PAYMENTS')")
    public ResponseEntity<Page<AdFreePassResponseDto>> getPayments(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        Page<AdFreePassResponseDto> passes = adFreePassRepository
                .findAllWithUser(PageRequest.of(page, size))
                .map(AdFreePassResponseDto::from);
        return ResponseEntity.ok(passes);
    }

    @PostMapping("/payments/{paymentIntentId}/refund")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Map<String, String>> refundPayment(
            @PathVariable String paymentIntentId,
            @AuthenticationPrincipal UserDetails userDetails) {
        stripeService.refundPayment(paymentIntentId);
        return ResponseEntity.ok(Map.of("message", "Refund initiated for " + paymentIntentId));
    }
}
