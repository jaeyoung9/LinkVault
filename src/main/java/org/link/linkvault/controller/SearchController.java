package org.link.linkvault.controller;

import lombok.RequiredArgsConstructor;
import org.link.linkvault.dto.BookmarkResponseDto;
import org.link.linkvault.dto.FolderResponseDto;
import org.link.linkvault.dto.TagResponseDto;
import org.link.linkvault.entity.User;
import org.link.linkvault.service.BookmarkService;
import org.link.linkvault.service.FolderService;
import org.link.linkvault.service.TagService;
import org.link.linkvault.service.UserService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final BookmarkService bookmarkService;
    private final TagService tagService;
    private final FolderService folderService;
    private final UserService userService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> search(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam String q) {
        User user = userService.getUserEntity(userDetails.getUsername());

        List<BookmarkResponseDto> bookmarks = bookmarkService.searchByKeyword(user, q,
                PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createdAt"))).getContent();
        List<TagResponseDto> tags = tagService.searchByName(q);
        List<FolderResponseDto> folders = folderService.searchByName(user, q);

        Map<String, Object> result = new HashMap<>();
        result.put("bookmarks", bookmarks);
        result.put("tags", tags);
        result.put("folders", folders);
        return ResponseEntity.ok(result);
    }
}
