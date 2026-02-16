package org.link.linkvault.dto;

import lombok.Builder;
import lombok.Getter;
import org.link.linkvault.entity.Folder;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
public class FolderResponseDto {

    private Long id;
    private String name;
    private Long parentId;
    private int displayOrder;
    private int bookmarkCount;
    private List<FolderResponseDto> children;

    public static FolderResponseDto from(Folder folder) {
        return FolderResponseDto.builder()
                .id(folder.getId())
                .name(folder.getName())
                .parentId(folder.getParent() != null ? folder.getParent().getId() : null)
                .displayOrder(folder.getDisplayOrder())
                .bookmarkCount(folder.getBookmarks().size())
                .children(folder.getChildren().stream()
                        .map(FolderResponseDto::from)
                        .collect(Collectors.toList()))
                .build();
    }
}
