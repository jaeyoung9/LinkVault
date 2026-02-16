package org.link.linkvault.service;

import lombok.RequiredArgsConstructor;
import org.link.linkvault.dto.SystemStatsDto;
import org.link.linkvault.entity.Tag;
import org.link.linkvault.entity.User;
import org.link.linkvault.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SystemStatsService {

    private final UserRepository userRepository;
    private final BookmarkRepository bookmarkRepository;
    private final TagRepository tagRepository;
    private final FolderRepository folderRepository;

    public SystemStatsDto getSystemStats() {
        long totalUsers = userRepository.count();
        long totalBookmarks = bookmarkRepository.count();
        long totalTags = tagRepository.count();
        long totalFolders = folderRepository.count();

        // Bookmarks per day (last 7 days)
        Map<String, Long> bookmarksPerDay = new LinkedHashMap<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM/dd");
        for (int i = 6; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            LocalDateTime start = date.atStartOfDay();
            LocalDateTime end = date.atTime(LocalTime.MAX);
            long count = bookmarkRepository.countByCreatedAtBetween(start, end);
            bookmarksPerDay.put(date.format(fmt), count);
        }

        // Top 10 tags by bookmark count
        Map<String, Long> topTags = new LinkedHashMap<>();
        List<Tag> tags = tagRepository.findAllOrderByBookmarkCountDesc();
        int tagLimit = Math.min(tags.size(), 10);
        for (int i = 0; i < tagLimit; i++) {
            Tag tag = tags.get(i);
            topTags.put(tag.getName(), (long) tag.getBookmarks().size());
        }

        // Most active users by bookmark count
        Map<String, Long> mostActiveUsers = new LinkedHashMap<>();
        List<User> users = userRepository.findAll();
        users.sort((a, b) -> Long.compare(
                userRepository.countBookmarksByUserId(b.getId()),
                userRepository.countBookmarksByUserId(a.getId())));
        int userLimit = Math.min(users.size(), 10);
        for (int i = 0; i < userLimit; i++) {
            User user = users.get(i);
            mostActiveUsers.put(user.getUsername(), userRepository.countBookmarksByUserId(user.getId()));
        }

        return SystemStatsDto.builder()
                .totalUsers(totalUsers)
                .totalBookmarks(totalBookmarks)
                .totalTags(totalTags)
                .totalFolders(totalFolders)
                .bookmarksPerDay(bookmarksPerDay)
                .topTags(topTags)
                .mostActiveUsers(mostActiveUsers)
                .build();
    }
}
