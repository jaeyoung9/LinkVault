package org.link.linkvault.controller;

import lombok.RequiredArgsConstructor;
import org.link.linkvault.dto.FavoriteBookmarkResponseDto;
import org.link.linkvault.dto.FavoriteReorderDto;
import org.link.linkvault.entity.User;
import org.link.linkvault.service.FavoriteBookmarkService;
import org.link.linkvault.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bookmarks")
@RequiredArgsConstructor
public class FavoriteBookmarkController {

    private final FavoriteBookmarkService favoriteBookmarkService;
    private final UserService userService;

    @PostMapping("/{id}/favorite")
    public ResponseEntity<Map<String, Boolean>> toggleFavorite(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        User user = userService.getUserEntity(userDetails.getUsername());
        boolean favorited = favoriteBookmarkService.toggleFavorite(user, id);
        return ResponseEntity.ok(Map.of("favorited", favorited));
    }

    @GetMapping("/favorites")
    public ResponseEntity<List<FavoriteBookmarkResponseDto>> getFavorites(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.getUserEntity(userDetails.getUsername());
        return ResponseEntity.ok(favoriteBookmarkService.getFavorites(user));
    }

    @PutMapping("/favorites/reorder")
    public ResponseEntity<Void> reorder(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody FavoriteReorderDto dto) {
        User user = userService.getUserEntity(userDetails.getUsername());
        favoriteBookmarkService.reorder(user, dto);
        return ResponseEntity.ok().build();
    }
}
