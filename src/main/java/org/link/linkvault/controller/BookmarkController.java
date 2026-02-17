package org.link.linkvault.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.link.linkvault.dto.BookmarkRequestDto;
import org.link.linkvault.dto.BookmarkResponseDto;
import org.link.linkvault.entity.Role;
import org.link.linkvault.entity.User;
import org.link.linkvault.service.BookmarkService;
import org.link.linkvault.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bookmarks")
@RequiredArgsConstructor
public class BookmarkController {

    private final BookmarkService bookmarkService;
    private final UserService userService;
    private final ObjectMapper objectMapper;

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
    public ResponseEntity<BookmarkResponseDto> getBookmark(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        return ResponseEntity.ok(bookmarkService.findById(id, getUser(userDetails)));
    }

    // JSON-only create (backward compatible)
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<BookmarkResponseDto> createBookmarkJson(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody BookmarkRequestDto requestDto) {
        BookmarkResponseDto created = bookmarkService.create(getUser(userDetails), requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // Multipart create (with photos)
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BookmarkResponseDto> createBookmarkMultipart(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestPart("data") String dataJson,
            @RequestPart(value = "photos", required = false) List<MultipartFile> photos) throws Exception {
        BookmarkRequestDto requestDto = objectMapper.readValue(dataJson, BookmarkRequestDto.class);
        BookmarkResponseDto created = bookmarkService.create(getUser(userDetails), requestDto, photos);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // JSON-only update (backward compatible)
    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<BookmarkResponseDto> updateBookmarkJson(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @Valid @RequestBody BookmarkRequestDto requestDto) {
        return ResponseEntity.ok(bookmarkService.update(getUser(userDetails), id, requestDto));
    }

    // Multipart update (with photos)
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BookmarkResponseDto> updateBookmarkMultipart(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @RequestPart("data") String dataJson,
            @RequestPart(value = "photos", required = false) List<MultipartFile> photos,
            @RequestPart(value = "deletePhotoIds", required = false) String deletePhotoIdsJson,
            @RequestPart(value = "photoOrder", required = false) String photoOrderJson) throws Exception {
        BookmarkRequestDto requestDto = objectMapper.readValue(dataJson, BookmarkRequestDto.class);
        List<Long> deletePhotoIds = null;
        if (deletePhotoIdsJson != null && !deletePhotoIdsJson.isBlank()) {
            deletePhotoIds = objectMapper.readValue(deletePhotoIdsJson,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, Long.class));
        }
        List<Long> photoOrder = null;
        if (photoOrderJson != null && !photoOrderJson.isBlank()) {
            photoOrder = objectMapper.readValue(photoOrderJson,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, Long.class));
        }
        return ResponseEntity.ok(bookmarkService.update(getUser(userDetails), id, requestDto, photos, deletePhotoIds, photoOrder));
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
    public ResponseEntity<BookmarkResponseDto> recordAccess(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        return ResponseEntity.ok(bookmarkService.recordAccess(id, getUser(userDetails)));
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

    // --- Map data ---

    @GetMapping("/map-data")
    public ResponseEntity<List<BookmarkResponseDto>> getMapData(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "false") boolean admin) {
        if (admin && userDetails != null) {
            User user = getUser(userDetails);
            if (user.getRole() == Role.SUPER_ADMIN || user.getRole() == Role.COMMUNITY_ADMIN
                    || user.getRole() == Role.MODERATOR) {
                return ResponseEntity.ok(bookmarkService.findAllWithLocationAdmin());
            }
        }
        return ResponseEntity.ok(bookmarkService.findAllWithLocation());
    }
}
