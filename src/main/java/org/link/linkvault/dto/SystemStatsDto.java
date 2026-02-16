package org.link.linkvault.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
public class SystemStatsDto {

    private long totalUsers;
    private long totalBookmarks;
    private long totalTags;
    private long totalFolders;
    private Map<String, Long> bookmarksPerDay;
    private Map<String, Long> topTags;
    private Map<String, Long> mostActiveUsers;
}
