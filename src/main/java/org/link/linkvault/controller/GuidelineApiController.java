package org.link.linkvault.controller;

import lombok.RequiredArgsConstructor;
import org.link.linkvault.dto.GuidelineStepResponseDto;
import org.link.linkvault.entity.User;
import org.link.linkvault.service.GuidelineStepService;
import org.link.linkvault.service.SystemSettingsService;
import org.link.linkvault.service.UserService;
import org.link.linkvault.service.UserSettingsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/guidelines")
@RequiredArgsConstructor
public class GuidelineApiController {

    private final GuidelineStepService guidelineStepService;
    private final SystemSettingsService systemSettingsService;
    private final UserSettingsService userSettingsService;
    private final UserService userService;

    @GetMapping("/screen/{screen}")
    public ResponseEntity<Map<String, Object>> getScreenGuidelines(
            @PathVariable String screen,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (userDetails == null) {
            return ResponseEntity.ok(Map.of("enabled", false));
        }

        boolean enabled = "true".equals(systemSettingsService.getValue("guideline.enabled").orElse("false"));
        if (!enabled) {
            return ResponseEntity.ok(Map.of("enabled", false));
        }

        User user = userService.getUserEntity(userDetails.getUsername());
        boolean completed = userSettingsService.isGuidelinesCompleted(user);
        boolean firstLoginOnly = "true".equals(systemSettingsService.getValue("guideline.first-login-only").orElse("true"));

        if (completed && firstLoginOnly) {
            return ResponseEntity.ok(Map.of("enabled", false, "completed", true));
        }

        // Check per-screen toggle
        boolean screenEnabled = "true".equals(
                systemSettingsService.getValue("guideline.screen." + screen + ".enabled").orElse("false"));

        List<GuidelineStepResponseDto> steps = guidelineStepService.findEnabledByScreen(screen);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("enabled", true);
        result.put("completed", completed);
        result.put("screenEnabled", screenEnabled);
        result.put("dismissible", "true".equals(systemSettingsService.getValue("guideline.dismissible").orElse("true")));
        result.put("displayMode", systemSettingsService.getValue("guideline.default-mode").orElse("TOOLTIP"));
        result.put("welcomeTitle", systemSettingsService.getValue("guideline.welcome.title").orElse("Welcome to LinkVault!"));
        result.put("welcomeDescription", systemSettingsService.getValue("guideline.welcome.description").orElse(""));
        result.put("steps", steps);
        return ResponseEntity.ok(result);
    }
}
