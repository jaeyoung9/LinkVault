package org.link.linkvault.controller;

import lombok.RequiredArgsConstructor;
import org.link.linkvault.dto.CommentRequestDto;
import org.link.linkvault.dto.CommentResponseDto;
import org.link.linkvault.dto.CommentVoteResponseDto;
import org.link.linkvault.entity.User;
import org.link.linkvault.entity.VoteType;
import org.link.linkvault.service.CommentService;
import org.link.linkvault.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;
    private final UserService userService;

    @GetMapping("/bookmark/{bookmarkId}")
    public ResponseEntity<List<CommentResponseDto>> getComments(
            @PathVariable Long bookmarkId,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.getUserEntity(userDetails.getUsername());
        return ResponseEntity.ok(commentService.getCommentsForBookmark(bookmarkId, user));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('COMMENT')")
    public ResponseEntity<CommentResponseDto> createComment(
            @Valid @RequestBody CommentRequestDto dto,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.getUserEntity(userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(commentService.create(dto, user));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('COMMENT')")
    public ResponseEntity<CommentResponseDto> updateComment(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.getUserEntity(userDetails.getUsername());
        boolean isModerator = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("MODERATE_COMMENTS"));
        return ResponseEntity.ok(commentService.update(id, body.get("content"), user, isModerator));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('COMMENT')")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.getUserEntity(userDetails.getUsername());
        boolean isModerator = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("MODERATE_COMMENTS"));
        commentService.delete(id, user, isModerator);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/vote")
    @PreAuthorize("hasAuthority('VOTE')")
    public ResponseEntity<CommentVoteResponseDto> vote(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.getUserEntity(userDetails.getUsername());
        VoteType voteType = VoteType.valueOf(body.get("voteType"));
        return ResponseEntity.ok(commentService.vote(id, voteType, user));
    }
}
