package org.link.linkvault.controller;

import lombok.RequiredArgsConstructor;
import org.link.linkvault.dto.BookmarkRequestDto;
import org.link.linkvault.dto.BookmarkResponseDto;
import org.link.linkvault.service.BookmarkService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/bookmarks")
@RequiredArgsConstructor
public class BookmarkController {

    private final BookmarkService bookmarkService;

    @GetMapping
    public ResponseEntity<List<BookmarkResponseDto>> getAllBookmarks() {
        return ResponseEntity.ok(bookmarkService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookmarkResponseDto> getBookmark(@PathVariable Long id) {
        return ResponseEntity.ok(bookmarkService.findById(id));
    }

    @PostMapping
    public ResponseEntity<BookmarkResponseDto> createBookmark(@Valid @RequestBody BookmarkRequestDto requestDto) {
        BookmarkResponseDto created = bookmarkService.create(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<BookmarkResponseDto> updateBookmark(
            @PathVariable Long id,
            @Valid @RequestBody BookmarkRequestDto requestDto) {
        return ResponseEntity.ok(bookmarkService.update(id, requestDto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBookmark(@PathVariable Long id) {
        bookmarkService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    public ResponseEntity<List<BookmarkResponseDto>> searchBookmarks(@RequestParam String keyword) {
        return ResponseEntity.ok(bookmarkService.searchByKeyword(keyword));
    }

    @GetMapping("/tag/{tagName}")
    public ResponseEntity<List<BookmarkResponseDto>> getBookmarksByTag(@PathVariable String tagName) {
        return ResponseEntity.ok(bookmarkService.findByTagName(tagName));
    }
}
