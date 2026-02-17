package org.link.linkvault.dto;

import lombok.Builder;
import lombok.Getter;
import org.link.linkvault.entity.Bookmark;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@Builder
public class BookmarkResponseDto {

    private Long id;
    private String title;
    private String url;
    private String description;
    private String favicon;
    private Set<String> tagNames;
    private Long folderId;
    private String folderName;
    private String ownerUsername;
    private boolean deleted;
    private int accessCount;
    private int commentCount;
    private LocalDateTime lastAccessedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // New fields
    private Double latitude;
    private Double longitude;
    private String address;
    private String caption;
    private String mapEmoji;
    private boolean privatePost;
    private List<PhotoResponseDto> photos;
    private String leadPhotoUrl;

    public static BookmarkResponseDto from(Bookmark bookmark) {
        List<PhotoResponseDto> photoDtos = (bookmark.getPhotos() != null && !bookmark.getPhotos().isEmpty())
                ? bookmark.getPhotos().stream()
                    .map(PhotoResponseDto::from)
                    .collect(Collectors.toList())
                : Collections.emptyList();

        String leadPhoto = photoDtos.isEmpty() ? null : photoDtos.get(0).getUrl();

        return BookmarkResponseDto.builder()
                .id(bookmark.getId())
                .title(bookmark.getTitle())
                .url(bookmark.getUrl())
                .description(bookmark.getDescription())
                .favicon(bookmark.getFavicon())
                .tagNames(bookmark.getTags().stream()
                        .map(tag -> tag.getName())
                        .collect(Collectors.toSet()))
                .folderId(bookmark.getFolder() != null ? bookmark.getFolder().getId() : null)
                .folderName(bookmark.getFolder() != null ? bookmark.getFolder().getName() : null)
                .ownerUsername(bookmark.getUser() != null ? bookmark.getUser().getUsername() : null)
                .deleted(bookmark.isDeleted())
                .accessCount(bookmark.getAccessCount())
                .commentCount(bookmark.getCommentCount())
                .lastAccessedAt(bookmark.getLastAccessedAt())
                .createdAt(bookmark.getCreatedAt())
                .updatedAt(bookmark.getUpdatedAt())
                .latitude(bookmark.getLatitude())
                .longitude(bookmark.getLongitude())
                .address(bookmark.getAddress())
                .caption(bookmark.getCaption())
                .mapEmoji(bookmark.getMapEmoji())
                .privatePost(bookmark.isPrivatePost())
                .photos(photoDtos)
                .leadPhotoUrl(leadPhoto)
                .build();
    }
}
