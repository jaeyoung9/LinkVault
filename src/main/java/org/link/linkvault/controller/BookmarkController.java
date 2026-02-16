package org.link.linkvault.controller;

import lombok.RequiredArgsConstructor;
import org.link.linkvault.dto.BookmarkRequestDto;
import org.link.linkvault.dto.BookmarkResponseDto;
import org.link.linkvault.entity.User;
import org.link.linkvault.service.BookmarkService;
import org.link.linkvault.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bookmarks")
@RequiredArgsConstructor
public class BookmarkController {

    private final BookmarkService bookmarkService;
    private final UserService userService;

    private User getUser(UserDetails userDetails) {
        return userService.getUserEntity(userDetails.getUsername());
    }

    @GetMapping
    public ResponseEntity<Page<BookmarkResponseDto>> getAllBookmarks(
            @AuthenticationPrincipal UserDetails userDetails,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(bookmarkService.findAll(getUser(userDetails), pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookmarkResponseDto> getBookmark(@PathVariable Long id) {
        return ResponseEntity.ok(bookmarkService.findById(id));
    }

    @PostMapping
    public ResponseEntity<BookmarkResponseDto> createBookmark(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody BookmarkRequestDto requestDto) {
        BookmarkResponseDto created = bookmarkService.create(getUser(userDetails), requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<BookmarkResponseDto> updateBookmark(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @Valid @RequestBody BookmarkRequestDto requestDto) {
        return ResponseEntity.ok(bookmarkService.update(getUser(userDetails), id, requestDto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBookmark(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        bookmarkService.delete(getUser(userDetails), id);
        return ResponseEntity.noContent().build();
    }

    // --- Search ---

    @GetMapping("/search")
    public ResponseEntity<Page<BookmarkResponseDto>> searchBookmarks(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam String keyword,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(bookmarkService.searchByKeyword(getUser(userDetails), keyword, pageable));
    }

    // --- Filter by tag ---

    @GetMapping("/tag/{tagName}")
    public ResponseEntity<List<BookmarkResponseDto>> getBookmarksByTag(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String tagName) {
        return ResponseEntity.ok(bookmarkService.findByTagName(getUser(userDetails), tagName));
    }

    // --- Filter by folder ---

    @GetMapping("/folder/{folderId}")
    public ResponseEntity<List<BookmarkResponseDto>> getBookmarksByFolder(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long folderId) {
        return ResponseEntity.ok(bookmarkService.findByFolderId(getUser(userDetails), folderId));
    }

    @GetMapping("/uncategorized")
    public ResponseEntity<List<BookmarkResponseDto>> getUncategorized(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(bookmarkService.findUncategorized(getUser(userDetails)));
    }

    // --- Access tracking ---

    @PostMapping("/{id}/access")
    public ResponseEntity<BookmarkResponseDto> recordAccess(@PathVariable Long id) {
        return ResponseEntity.ok(bookmarkService.recordAccess(id));
    }

    @GetMapping("/frequent")
    public ResponseEntity<List<BookmarkResponseDto>> getFrequent(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(bookmarkService.findFrequentlyAccessed(getUser(userDetails), limit));
    }

    @GetMapping("/recent")
    public ResponseEntity<List<BookmarkResponseDto>> getRecent(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(bookmarkService.findRecentlyAccessed(limit));
    }

    // --- Move to folder (drag-and-drop) ---

    @PatchMapping("/{id}/move")
    public ResponseEntity<BookmarkResponseDto> moveToFolder(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @RequestBody Map<String, Long> body) {
        Long folderId = body.get("folderId");
        return ResponseEntity.ok(bookmarkService.moveToFolder(getUser(userDetails), id, folderId));
    }

    // --- Duplicate URL check ---

    @GetMapping("/check-url")
    public ResponseEntity<Map<String, Boolean>> checkUrl(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam String url) {
        return ResponseEntity.ok(Map.of("exists", bookmarkService.existsByUrl(getUser(userDetails), url)));
    }
}
