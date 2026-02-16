package org.link.linkvault.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookmarkExportDto {

    private String title;
    private String url;
    private String description;
    private Set<String> tagNames;
    private String folderPath;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ExportWrapper {
        private String exportDate;
        private int totalBookmarks;
        private List<BookmarkExportDto> bookmarks;
    }
}
