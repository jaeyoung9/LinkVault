package org.link.linkvault.controller;

import lombok.RequiredArgsConstructor;
import org.link.linkvault.dto.*;
import org.link.linkvault.entity.User;
import org.link.linkvault.service.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminApiController {

    private final UserService userService;
    private final BookmarkService bookmarkService;
    private final TagService tagService;
    private final AuditLogService auditLogService;
    private final SystemStatsService systemStatsService;
    private final DatabaseBackupService databaseBackupService;

    // --- User CRUD ---

    @PostMapping("/users")
    public ResponseEntity<UserResponseDto> createUser(@Valid @RequestBody UserRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.create(dto));
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<UserResponseDto> updateUser(@PathVariable Long id, @Valid @RequestBody UserRequestDto dto) {
        return ResponseEntity.ok(userService.update(id, dto));
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/users/{id}/toggle")
    public ResponseEntity<UserResponseDto> toggleUser(@PathVariable Long id) {
        return ResponseEntity.ok(userService.toggleEnabled(id));
    }

    // --- Tag Management ---

    @PostMapping("/tags/merge")
    public ResponseEntity<Map<String, String>> mergeTags(@RequestBody TagMergeRequestDto dto) {
        tagService.mergeTags(dto.getSourceTagIds(), dto.getTargetTagName());
        return ResponseEntity.ok(Map.of("message", "Tags merged successfully"));
    }

    @DeleteMapping("/tags/unused")
    public ResponseEntity<Map<String, Object>> deleteUnusedTags() {
        int count = tagService.deleteUnusedTags();
        return ResponseEntity.ok(Map.of("deleted", count, "message", count + " unused tags deleted"));
    }

    @DeleteMapping("/tags/{id}")
    public ResponseEntity<Void> deleteTag(@PathVariable Long id) {
        tagService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // --- Stats ---

    @GetMapping("/stats")
    public ResponseEntity<SystemStatsDto> getStats() {
        return ResponseEntity.ok(systemStatsService.getSystemStats());
    }

    // --- Bookmark Management ---

    @DeleteMapping("/bookmarks/{id}")
    public ResponseEntity<Void> deleteBookmark(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        User admin = userService.getUserEntity(userDetails.getUsername());
        bookmarkService.delete(admin, id);
        return ResponseEntity.noContent().build();
    }

    // --- Backup/Restore ---

    @PostMapping("/backup")
    public ResponseEntity<Map<String, String>> createBackup() {
        String filename = databaseBackupService.createBackup();
        return ResponseEntity.ok(Map.of("filename", filename, "message", "Backup created: " + filename));
    }

    @GetMapping("/backups")
    public ResponseEntity<java.util.List<String>> listBackups() {
        return ResponseEntity.ok(databaseBackupService.listBackups());
    }

    @PostMapping("/restore")
    public ResponseEntity<Map<String, String>> restoreBackup(@RequestBody Map<String, String> body) {
        String filename = body.get("filename");
        databaseBackupService.restoreBackup(filename);
        return ResponseEntity.ok(Map.of("message", "Database restored from: " + filename));
    }
}
