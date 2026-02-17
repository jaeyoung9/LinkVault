package org.link.linkvault.dto;

import lombok.Builder;
import lombok.Getter;
import org.link.linkvault.entity.PostPhoto;

@Getter
@Builder
public class PhotoResponseDto {

    private Long id;
    private String url;
    private String originalFilename;
    private int displayOrder;

    public static PhotoResponseDto from(PostPhoto photo) {
        String filename = photo.getStoragePath();
        return PhotoResponseDto.builder()
                .id(photo.getId())
                .url("/files/photos/" + filename)
                .originalFilename(photo.getOriginalFilename())
                .displayOrder(photo.getDisplayOrder())
                .build();
    }
}
