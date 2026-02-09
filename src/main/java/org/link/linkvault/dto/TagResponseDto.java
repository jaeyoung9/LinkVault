package org.link.linkvault.dto;

import lombok.Builder;
import lombok.Getter;
import org.link.linkvault.entity.Tag;

@Getter
@Builder
public class TagResponseDto {

    private Long id;
    private String name;
    private int bookmarkCount;

    public static TagResponseDto from(Tag tag) {
        return TagResponseDto.builder()
                .id(tag.getId())
                .name(tag.getName())
                .bookmarkCount(tag.getBookmarks().size())
                .build();
    }
}
