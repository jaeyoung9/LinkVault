package org.link.linkvault.controller;

import lombok.RequiredArgsConstructor;
import org.link.linkvault.dto.*;
import org.link.linkvault.entity.Role;
import org.link.linkvault.entity.User;
import org.link.linkvault.service.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminApiController {

    private final UserService userService;
    private final BookmarkService bookmarkService;
    private final TagService tagService;
    private final AuditLogService auditLogService;
    private final SystemStatsService systemStatsService;
    private final DatabaseBackupService databaseBackupService;
    private final PermissionService permissionService;
    private final MenuService menuService;
    private final InvitationService invitationService;
    private final CommentService commentService;
    private final QnaArticleService qnaArticleService;
    private final AnnouncementService announcementService;

    // --- User CRUD ---

    @PostMapping("/users")
    @PreAuthorize("hasAuthority('MANAGE_USERS')")
    public ResponseEntity<UserResponseDto> createUser(@Valid @RequestBody UserRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.create(dto));
    }

    @PutMapping("/users/{id}")
    @PreAuthorize("hasAuthority('MANAGE_USERS')")
    public ResponseEntity<UserResponseDto> updateUser(@PathVariable Long id, @Valid @RequestBody UserRequestDto dto) {
        return ResponseEntity.ok(userService.update(id, dto));
    }

    @DeleteMapping("/users/{id}")
    @PreAuthorize("hasAuthority('MANAGE_USERS')")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/users/{id}/toggle")
    @PreAuthorize("hasAuthority('MANAGE_USERS')")
    public ResponseEntity<UserResponseDto> toggleUser(@PathVariable Long id) {
        return ResponseEntity.ok(userService.toggleEnabled(id));
    }

    // --- Tag Management ---

    @PostMapping("/tags/merge")
    @PreAuthorize("hasAuthority('MANAGE_TAGS')")
    public ResponseEntity<Map<String, String>> mergeTags(@RequestBody TagMergeRequestDto dto) {
        tagService.mergeTags(dto.getSourceTagIds(), dto.getTargetTagName());
        return ResponseEntity.ok(Map.of("message", "Tags merged successfully"));
    }

    @DeleteMapping("/tags/unused")
    @PreAuthorize("hasAuthority('MANAGE_TAGS')")
    public ResponseEntity<Map<String, Object>> deleteUnusedTags() {
        int count = tagService.deleteUnusedTags();
        return ResponseEntity.ok(Map.of("deleted", count, "message", count + " unused tags deleted"));
    }

    @DeleteMapping("/tags/{id}")
    @PreAuthorize("hasAuthority('MANAGE_TAGS')")
    public ResponseEntity<Void> deleteTag(@PathVariable Long id) {
        tagService.delete(id);
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
    @PreAuthorize("hasAuthority('MANAGE_BOOKMARKS')")
    public ResponseEntity<Void> deleteBookmark(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        User admin = userService.getUserEntity(userDetails.getUsername());
        bookmarkService.delete(admin, id);
        return ResponseEntity.noContent().build();
    }

    // --- Backup/Restore ---

    @PostMapping("/backup")
    @PreAuthorize("hasAuthority('MANAGE_BACKUP')")
    public ResponseEntity<Map<String, String>> createBackup() {
        String filename = databaseBackupService.createBackup();
        return ResponseEntity.ok(Map.of("filename", filename, "message", "Backup created: " + filename));
    }

    @GetMapping("/backups")
    @PreAuthorize("hasAuthority('MANAGE_BACKUP')")
    public ResponseEntity<java.util.List<String>> listBackups() {
        return ResponseEntity.ok(databaseBackupService.listBackups());
    }

    @PostMapping("/restore")
    @PreAuthorize("hasAuthority('MANAGE_BACKUP')")
    public ResponseEntity<Map<String, String>> restoreBackup(@RequestBody Map<String, String> body) {
        String filename = body.get("filename");
        databaseBackupService.restoreBackup(filename);
        return ResponseEntity.ok(Map.of("message", "Database restored from: " + filename));
    }

    // --- Comment Moderation ---

    @GetMapping("/comments")
    @PreAuthorize("hasAuthority('MODERATE_COMMENTS')")
    public ResponseEntity<List<CommentResponseDto>> getAllComments() {
        return ResponseEntity.ok(commentService.getAllComments());
    }

    @DeleteMapping("/comments/{id}")
    @PreAuthorize("hasAuthority('MODERATE_COMMENTS')")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        User moderator = userService.getUserEntity(userDetails.getUsername());
        commentService.delete(id, moderator, true);
        return ResponseEntity.noContent().build();
    }

    // --- Invitation Management ---

    @GetMapping("/invitations")
    @PreAuthorize("hasAuthority('MANAGE_INVITATIONS')")
    public ResponseEntity<List<InvitationCodeResponseDto>> getInvitations() {
        return ResponseEntity.ok(invitationService.findAll());
    }

    @PostMapping("/invitations")
    @PreAuthorize("hasAuthority('MANAGE_INVITATIONS')")
    public ResponseEntity<InvitationCodeResponseDto> createInvitation(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody InvitationCodeRequestDto dto) {
        User creator = userService.getUserEntity(userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(invitationService.create(dto, creator));
    }

    @PatchMapping("/invitations/{id}/toggle")
    @PreAuthorize("hasAuthority('MANAGE_INVITATIONS')")
    public ResponseEntity<Map<String, String>> toggleInvitation(@PathVariable Long id) {
        invitationService.toggleActive(id);
        return ResponseEntity.ok(Map.of("message", "Invitation toggled"));
    }

    @DeleteMapping("/invitations/{id}")
    @PreAuthorize("hasAuthority('MANAGE_INVITATIONS')")
    public ResponseEntity<Void> deleteInvitation(@PathVariable Long id) {
        invitationService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // --- Menu Management ---

    @GetMapping("/menus")
    @PreAuthorize("hasAuthority('MANAGE_MENUS')")
    public ResponseEntity<List<MenuItemResponseDto>> getMenuItems(
            @RequestParam org.link.linkvault.entity.MenuType menuType) {
        return ResponseEntity.ok(menuService.getAllMenuItems(menuType));
    }

    @PostMapping("/menus")
    @PreAuthorize("hasAuthority('MANAGE_MENUS')")
    public ResponseEntity<MenuItemResponseDto> createMenuItem(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody MenuItemRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(menuService.create(dto, userDetails.getUsername()));
    }

    @PutMapping("/menus/{id}")
    @PreAuthorize("hasAuthority('MANAGE_MENUS')")
    public ResponseEntity<MenuItemResponseDto> updateMenuItem(
            @PathVariable Long id, @Valid @RequestBody MenuItemRequestDto dto) {
        return ResponseEntity.ok(menuService.update(id, dto));
    }

    @DeleteMapping("/menus/{id}")
    @PreAuthorize("hasAuthority('MANAGE_MENUS')")
    public ResponseEntity<Void> deleteMenuItem(@PathVariable Long id) {
        menuService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/menus/{id}/toggle")
    @PreAuthorize("hasAuthority('MANAGE_MENUS')")
    public ResponseEntity<Map<String, String>> toggleMenuVisibility(@PathVariable Long id) {
        menuService.toggleVisibility(id);
        return ResponseEntity.ok(Map.of("message", "Menu visibility toggled"));
    }

    @PatchMapping("/menus/reorder")
    @PreAuthorize("hasAuthority('MANAGE_MENUS')")
    public ResponseEntity<Map<String, String>> reorderMenuItems(
            @RequestBody List<MenuItemOrderDto> orders) {
        menuService.reorder(orders);
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
    public ResponseEntity<Map<String, String>> togglePermission(@Valid @RequestBody RolePermissionRequestDto dto) {
        permissionService.togglePermission(dto.getRole(), dto.getPermissionId(), dto.isGranted());
        return ResponseEntity.ok(Map.of("message", "Permission updated"));
    }

    // --- QnA Management ---

    @PostMapping("/qna")
    @PreAuthorize("hasAuthority('MANAGE_QNA')")
    public ResponseEntity<QnaArticleResponseDto> createQnaArticle(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody QnaArticleRequestDto dto) {
        User creator = userService.getUserEntity(userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(qnaArticleService.create(dto, creator));
    }

    @PutMapping("/qna/{id}")
    @PreAuthorize("hasAuthority('MANAGE_QNA')")
    public ResponseEntity<QnaArticleResponseDto> updateQnaArticle(
            @PathVariable Long id, @Valid @RequestBody QnaArticleRequestDto dto) {
        return ResponseEntity.ok(qnaArticleService.update(id, dto));
    }

    @PatchMapping("/qna/{id}/status")
    @PreAuthorize("hasAuthority('MANAGE_QNA')")
    public ResponseEntity<Map<String, String>> updateQnaStatus(
            @PathVariable Long id, @RequestBody Map<String, String> body) {
        org.link.linkvault.entity.QnaStatus status = org.link.linkvault.entity.QnaStatus.valueOf(body.get("status"));
        qnaArticleService.updateStatus(id, status);
        return ResponseEntity.ok(Map.of("message", "Status updated"));
    }

    @DeleteMapping("/qna/{id}")
    @PreAuthorize("hasAuthority('MANAGE_QNA')")
    public ResponseEntity<Void> deleteQnaArticle(@PathVariable Long id) {
        qnaArticleService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // --- Announcement Management ---

    @PostMapping("/announcements")
    @PreAuthorize("hasAuthority('MANAGE_ANNOUNCEMENTS')")
    public ResponseEntity<AnnouncementResponseDto> createAnnouncement(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody AnnouncementRequestDto dto) {
        User creator = userService.getUserEntity(userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(announcementService.create(dto, creator));
    }

    @PutMapping("/announcements/{id}")
    @PreAuthorize("hasAuthority('MANAGE_ANNOUNCEMENTS')")
    public ResponseEntity<AnnouncementResponseDto> updateAnnouncement(
            @PathVariable Long id, @Valid @RequestBody AnnouncementRequestDto dto) {
        return ResponseEntity.ok(announcementService.update(id, dto));
    }

    @PatchMapping("/announcements/{id}/status")
    @PreAuthorize("hasAuthority('MANAGE_ANNOUNCEMENTS')")
    public ResponseEntity<Map<String, String>> updateAnnouncementStatus(
            @PathVariable Long id, @RequestBody Map<String, String> body) {
        org.link.linkvault.entity.AnnouncementStatus status = org.link.linkvault.entity.AnnouncementStatus.valueOf(body.get("status"));
        announcementService.updateStatus(id, status);
        return ResponseEntity.ok(Map.of("message", "Status updated"));
    }

    @DeleteMapping("/announcements/{id}")
    @PreAuthorize("hasAuthority('MANAGE_ANNOUNCEMENTS')")
    public ResponseEntity<Void> deleteAnnouncement(@PathVariable Long id) {
        announcementService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
