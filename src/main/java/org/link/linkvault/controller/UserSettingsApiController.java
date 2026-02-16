package org.link.linkvault.controller;

import lombok.RequiredArgsConstructor;
import org.link.linkvault.dto.ChangePasswordRequestDto;
import org.link.linkvault.dto.UserSettingsResponseDto;
import org.link.linkvault.entity.ProfileVisibility;
import org.link.linkvault.entity.Theme;
import org.link.linkvault.entity.User;
import org.link.linkvault.service.UserService;
import org.link.linkvault.service.UserSettingsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Map;

@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class UserSettingsApiController {

    private final UserSettingsService userSettingsService;
    private final UserService userService;

    @GetMapping
    public ResponseEntity<UserSettingsResponseDto> getSettings(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.getUserEntity(userDetails.getUsername());
        return ResponseEntity.ok(userSettingsService.getSettings(user));
    }

    @PutMapping("/theme")
    public ResponseEntity<UserSettingsResponseDto> updateTheme(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody Map<String, String> body) {
        User user = userService.getUserEntity(userDetails.getUsername());
        Theme theme = Theme.valueOf(body.get("theme"));
        return ResponseEntity.ok(userSettingsService.updateTheme(user, theme));
    }

    @PutMapping("/notifications")
    public ResponseEntity<UserSettingsResponseDto> updateNotifications(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody Map<String, Boolean> body) {
        User user = userService.getUserEntity(userDetails.getUsername());
        boolean email = body.getOrDefault("emailNotifications", true);
        boolean browser = body.getOrDefault("browserNotifications", false);
        return ResponseEntity.ok(userSettingsService.updateNotifications(user, email, browser));
    }

    @PutMapping("/privacy")
    public ResponseEntity<UserSettingsResponseDto> updatePrivacy(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody Map<String, String> body) {
        User user = userService.getUserEntity(userDetails.getUsername());
        ProfileVisibility visibility = ProfileVisibility.valueOf(body.getOrDefault("profileVisibility", "PUBLIC"));
        boolean showEmail = Boolean.parseBoolean(body.getOrDefault("showEmail", "false"));
        return ResponseEntity.ok(userSettingsService.updatePrivacy(user, visibility, showEmail));
    }

    @PostMapping("/change-password")
    public ResponseEntity<Map<String, String>> changePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ChangePasswordRequestDto dto) {
        User user = userService.getUserEntity(userDetails.getUsername());
        userSettingsService.changePassword(user, dto.getCurrentPassword(), dto.getNewPassword(), dto.getConfirmPassword());
        return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
    }

    @PostMapping("/change-email")
    public ResponseEntity<Map<String, String>> changeEmail(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody Map<String, String> body) {
        User user = userService.getUserEntity(userDetails.getUsername());
        userSettingsService.changeEmail(user, body.get("email"));
        return ResponseEntity.ok(Map.of("message", "Email changed successfully"));
    }
}
