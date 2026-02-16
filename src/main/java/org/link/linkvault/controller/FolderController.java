package org.link.linkvault.controller;

import lombok.RequiredArgsConstructor;
import org.link.linkvault.dto.FolderMoveDto;
import org.link.linkvault.dto.FolderRequestDto;
import org.link.linkvault.dto.FolderResponseDto;
import org.link.linkvault.entity.User;
import org.link.linkvault.service.FolderService;
import org.link.linkvault.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/folders")
@RequiredArgsConstructor
public class FolderController {

    private final FolderService folderService;
    private final UserService userService;

    private User getUser(UserDetails userDetails) {
        return userService.getUserEntity(userDetails.getUsername());
    }

    @GetMapping
    public ResponseEntity<List<FolderResponseDto>> getRootFolders(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(folderService.findRootFolders(getUser(userDetails)));
    }

    @GetMapping("/all")
    public ResponseEntity<List<FolderResponseDto>> getAllFolders(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(folderService.findAll(getUser(userDetails)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FolderResponseDto> getFolder(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        return ResponseEntity.ok(folderService.findById(getUser(userDetails), id));
    }

    @PostMapping
    public ResponseEntity<FolderResponseDto> createFolder(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody FolderRequestDto requestDto) {
        FolderResponseDto created = folderService.create(getUser(userDetails), requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<FolderResponseDto> updateFolder(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @Valid @RequestBody FolderRequestDto requestDto) {
        return ResponseEntity.ok(folderService.update(getUser(userDetails), id, requestDto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFolder(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        folderService.delete(getUser(userDetails), id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/move")
    public ResponseEntity<FolderResponseDto> moveFolder(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @RequestBody FolderMoveDto moveDto) {
        return ResponseEntity.ok(folderService.move(getUser(userDetails), id, moveDto));
    }
}
