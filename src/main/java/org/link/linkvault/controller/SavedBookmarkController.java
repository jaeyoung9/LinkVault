package org.link.linkvault.controller;

import lombok.RequiredArgsConstructor;
import org.link.linkvault.dto.BookmarkResponseDto;
import org.link.linkvault.entity.User;
import org.link.linkvault.service.SavedBookmarkService;
import org.link.linkvault.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bookmarks")
@RequiredArgsConstructor
public class SavedBookmarkController {

    private final SavedBookmarkService savedBookmarkService;
    private final UserService userService;

    @PostMapping("/{id}/save")
    public ResponseEntity<Map<String, Object>> toggleSave(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        User user = userService.getUserEntity(userDetails.getUsername());
        boolean saved = savedBookmarkService.toggleSave(user, id);
        long count = savedBookmarkService.getCount(user);
        Map<String, Object> result = new HashMap<>();
        result.put("saved", saved);
        result.put("count", count);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/saved")
    public ResponseEntity<List<BookmarkResponseDto>> getSavedBookmarks(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.getUserEntity(userDetails.getUsername());
        return ResponseEntity.ok(savedBookmarkService.getSavedBookmarks(user));
    }
}
