package org.link.linkvault.controller;

import lombok.RequiredArgsConstructor;
import org.link.linkvault.dto.RewardStatusResponseDto;
import org.link.linkvault.entity.User;
import org.link.linkvault.service.RewardService;
import org.link.linkvault.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.Map;

@RestController
@RequestMapping("/api/reward")
@RequiredArgsConstructor
public class RewardApiController {

    private final RewardService rewardService;
    private final UserService userService;

    @GetMapping("/status")
    public ResponseEntity<RewardStatusResponseDto> getStatus(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.getUserEntity(userDetails.getUsername());
        return ResponseEntity.ok(rewardService.getStatus(user));
    }

    @PostMapping("/request-video")
    public ResponseEntity<?> requestVideo(@AuthenticationPrincipal UserDetails userDetails, HttpSession session) {
        User user = userService.getUserEntity(userDetails.getUsername());
        Map<String, Object> result = rewardService.requestVideo(user, session);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/video-complete")
    public ResponseEntity<?> videoComplete(@AuthenticationPrincipal UserDetails userDetails,
                                            @RequestBody Map<String, String> body,
                                            HttpSession session) {
        User user = userService.getUserEntity(userDetails.getUsername());
        String nonce = body.get("nonce");
        Map<String, Object> result = rewardService.completeVideo(user, nonce, session);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/redeem")
    public ResponseEntity<?> redeem(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.getUserEntity(userDetails.getUsername());
        Map<String, Object> result = rewardService.redeem(user);
        return ResponseEntity.ok(result);
    }
}
