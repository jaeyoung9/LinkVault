package org.link.linkvault.dto;

import lombok.Builder;
import lombok.Getter;
import org.link.linkvault.entity.SavedBookmark;

import java.time.LocalDateTime;

@Getter
@Builder
public class SavedBookmarkResponseDto {

    private BookmarkResponseDto bookmark;
    private LocalDateTime savedAt;

    public static SavedBookmarkResponseDto from(SavedBookmark savedBookmark) {
        return SavedBookmarkResponseDto.builder()
                .bookmark(BookmarkResponseDto.from(savedBookmark.getBookmark()))
                .savedAt(savedBookmark.getCreatedAt())
                .build();
    }
}
