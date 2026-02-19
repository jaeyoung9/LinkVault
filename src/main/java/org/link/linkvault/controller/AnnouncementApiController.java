package org.link.linkvault.controller;

import lombok.RequiredArgsConstructor;
import org.link.linkvault.dto.AnnouncementResponseDto;
import org.link.linkvault.entity.User;
import org.link.linkvault.entity.VoteType;
import org.link.linkvault.service.AnnouncementService;
import org.link.linkvault.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/announcements")
@RequiredArgsConstructor
public class AnnouncementApiController {

    private final AnnouncementService announcementService;
    private final UserService userService;

    @GetMapping
    public ResponseEntity<List<AnnouncementResponseDto>> listForUser(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.getUserEntity(userDetails.getUsername());
        return ResponseEntity.ok(announcementService.findVisibleForUser(user));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AnnouncementResponseDto> getAnnouncement(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.getUserEntity(userDetails.getUsername());
        AnnouncementResponseDto dto = announcementService.findById(id, user);
        announcementService.markAsRead(id, user);
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/{id}/acknowledge")
    public ResponseEntity<Map<String, String>> acknowledge(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.getUserEntity(userDetails.getUsername());
        announcementService.acknowledge(id, user);
        return ResponseEntity.ok(Map.of("message", "Acknowledged"));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> unreadCount(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.getUserEntity(userDetails.getUsername());
        return ResponseEntity.ok(Map.of("count", announcementService.getUnreadCountForUser(user)));
    }

    @PostMapping("/{id}/vote")
    public ResponseEntity<AnnouncementResponseDto> vote(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.getUserEntity(userDetails.getUsername());
        VoteType voteType = VoteType.valueOf(body.get("voteType"));
        return ResponseEntity.ok(announcementService.vote(id, voteType, user));
    }

    @PostMapping("/{id}/poll-vote")
    public ResponseEntity<AnnouncementResponseDto> pollVote(
            @PathVariable Long id,
            @RequestBody Map<String, Long> body,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.getUserEntity(userDetails.getUsername());
        Long optionId = body.get("optionId");
        return ResponseEntity.ok(announcementService.pollVote(id, optionId, user));
    }
}
