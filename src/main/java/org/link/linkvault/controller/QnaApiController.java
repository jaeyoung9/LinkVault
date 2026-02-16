package org.link.linkvault.controller;

import lombok.RequiredArgsConstructor;
import org.link.linkvault.dto.QnaArticleResponseDto;
import org.link.linkvault.dto.QnaFeedbackResponseDto;
import org.link.linkvault.entity.User;
import org.link.linkvault.service.QnaArticleService;
import org.link.linkvault.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/qna")
@RequiredArgsConstructor
public class QnaApiController {

    private final QnaArticleService qnaArticleService;
    private final UserService userService;

    @GetMapping
    public ResponseEntity<?> listPublished(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (keyword != null && !keyword.isBlank()) {
            Page<QnaArticleResponseDto> results = qnaArticleService.searchPublished(keyword, PageRequest.of(page, size));
            return ResponseEntity.ok(results);
        }
        if (category != null && !category.isBlank()) {
            return ResponseEntity.ok(qnaArticleService.findPublishedByCategory(category));
        }
        return ResponseEntity.ok(qnaArticleService.findAllPublished());
    }

    @GetMapping("/{id}")
    public ResponseEntity<QnaArticleResponseDto> getArticle(@PathVariable Long id) {
        return ResponseEntity.ok(qnaArticleService.findPublishedById(id));
    }

    @GetMapping("/{id}/feedback")
    public ResponseEntity<QnaFeedbackResponseDto> getFeedback(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.getUserEntity(userDetails.getUsername());
        return ResponseEntity.ok(qnaArticleService.getFeedback(id, user));
    }

    @PostMapping("/{id}/feedback")
    public ResponseEntity<QnaFeedbackResponseDto> submitFeedback(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody Map<String, Boolean> body) {
        User user = userService.getUserEntity(userDetails.getUsername());
        boolean helpful = body.getOrDefault("helpful", true);
        return ResponseEntity.ok(qnaArticleService.submitFeedback(id, user, helpful));
    }
}
