package org.link.linkvault.dto;

import lombok.Builder;
import lombok.Getter;
import org.link.linkvault.entity.FavoriteBookmark;

@Getter
@Builder
public class FavoriteBookmarkResponseDto {

    private Long bookmarkId;
    private String bookmarkTitle;
    private String bookmarkUrl;
    private String bookmarkFavicon;
    private int displayOrder;

    public static FavoriteBookmarkResponseDto from(FavoriteBookmark fb) {
        return FavoriteBookmarkResponseDto.builder()
                .bookmarkId(fb.getBookmark().getId())
                .bookmarkTitle(fb.getBookmark().getTitle())
                .bookmarkUrl(fb.getBookmark().getUrl())
                .bookmarkFavicon(fb.getBookmark().getFavicon())
                .displayOrder(fb.getDisplayOrder())
                .build();
    }
}
