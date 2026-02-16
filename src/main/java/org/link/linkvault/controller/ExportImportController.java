package org.link.linkvault.controller;

import lombok.RequiredArgsConstructor;
import org.link.linkvault.entity.User;
import org.link.linkvault.service.ExportImportService;
import org.link.linkvault.service.UserService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
@RequestMapping("/api/data")
@RequiredArgsConstructor
public class ExportImportController {

    private final ExportImportService exportImportService;
    private final UserService userService;

    private User getUser(UserDetails userDetails) {
        return userService.getUserEntity(userDetails.getUsername());
    }

    @GetMapping("/export/json")
    public ResponseEntity<byte[]> exportJson(@AuthenticationPrincipal UserDetails userDetails) throws Exception {
        String json = exportImportService.exportToJson(getUser(userDetails));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=linkvault-bookmarks.json")
                .contentType(MediaType.APPLICATION_JSON)
                .body(json.getBytes(StandardCharsets.UTF_8));
    }

    @GetMapping("/export/html")
    public ResponseEntity<byte[]> exportHtml(@AuthenticationPrincipal UserDetails userDetails) {
        String html = exportImportService.exportToHtml(getUser(userDetails));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=linkvault-bookmarks.html")
                .contentType(MediaType.TEXT_HTML)
                .body(html.getBytes(StandardCharsets.UTF_8));
    }

    @PostMapping("/import/json")
    public ResponseEntity<Map<String, Object>> importJson(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam("file") MultipartFile file) throws Exception {
        String json = new String(file.getBytes(), StandardCharsets.UTF_8);
        int count = exportImportService.importFromJson(getUser(userDetails), json);
        return ResponseEntity.ok(Map.of("imported", count, "message", count + " bookmarks imported successfully"));
    }

    @PostMapping("/import/html")
    public ResponseEntity<Map<String, Object>> importHtml(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam("file") MultipartFile file) throws Exception {
        String html = new String(file.getBytes(), StandardCharsets.UTF_8);
        int count = exportImportService.importFromHtml(getUser(userDetails), html);
        return ResponseEntity.ok(Map.of("imported", count, "message", count + " bookmarks imported from HTML"));
    }
}
