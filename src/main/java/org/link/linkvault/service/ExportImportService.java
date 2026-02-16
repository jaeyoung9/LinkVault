package org.link.linkvault.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.link.linkvault.dto.BookmarkExportDto;
import org.link.linkvault.entity.Bookmark;
import org.link.linkvault.entity.Folder;
import org.link.linkvault.entity.Role;
import org.link.linkvault.entity.Tag;
import org.link.linkvault.entity.User;
import org.link.linkvault.repository.BookmarkRepository;
import org.link.linkvault.repository.FolderRepository;
import org.link.linkvault.repository.TagRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExportImportService {

    private final BookmarkRepository bookmarkRepository;
    private final TagRepository tagRepository;
    private final FolderRepository folderRepository;
    private final FolderService folderService;
    private final ObjectMapper objectMapper;

    // --- JSON Export ---

    @Transactional(readOnly = true)
    public String exportToJson(User currentUser) throws Exception {
        List<Bookmark> bookmarks;
        if (currentUser.getRole() == Role.ADMIN) {
            bookmarks = bookmarkRepository.findAllWithTagsAndFolder();
        } else {
            bookmarks = bookmarkRepository.findByUserId(currentUser.getId(), Pageable.unpaged()).getContent();
        }

        List<BookmarkExportDto> exportList = bookmarks.stream()
                .map(b -> BookmarkExportDto.builder()
                        .title(b.getTitle())
                        .url(b.getUrl())
                        .description(b.getDescription())
                        .tagNames(b.getTags().stream().map(Tag::getName).collect(Collectors.toSet()))
                        .folderPath(folderService.buildFolderPath(b.getFolder()))
                        .build())
                .collect(Collectors.toList());

        BookmarkExportDto.ExportWrapper wrapper = BookmarkExportDto.ExportWrapper.builder()
                .exportDate(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .totalBookmarks(exportList.size())
                .bookmarks(exportList)
                .build();

        ObjectMapper prettyMapper = objectMapper.copy().enable(SerializationFeature.INDENT_OUTPUT);
        return prettyMapper.writeValueAsString(wrapper);
    }

    // --- JSON Import ---

    @Transactional
    public int importFromJson(User currentUser, String json) throws Exception {
        BookmarkExportDto.ExportWrapper wrapper = objectMapper.readValue(json,
                new TypeReference<BookmarkExportDto.ExportWrapper>() {});

        int imported = 0;
        for (BookmarkExportDto dto : wrapper.getBookmarks()) {
            if (bookmarkRepository.existsByUrlAndUserId(dto.getUrl(), currentUser.getId())) {
                log.info("Skipping duplicate URL: {}", dto.getUrl());
                continue;
            }

            Folder folder = resolveOrCreateFolderPath(dto.getFolderPath(), currentUser);

            Bookmark bookmark = Bookmark.builder()
                    .title(dto.getTitle())
                    .url(dto.getUrl())
                    .description(dto.getDescription())
                    .folder(folder)
                    .user(currentUser)
                    .build();

            if (dto.getTagNames() != null) {
                for (String tagName : dto.getTagNames()) {
                    Tag tag = tagRepository.findByName(tagName)
                            .orElseGet(() -> tagRepository.save(new Tag(tagName)));
                    bookmark.addTag(tag);
                }
            }

            bookmarkRepository.save(bookmark);
            imported++;
        }
        return imported;
    }

    // --- HTML Bookmark Export (Netscape format) ---

    @Transactional(readOnly = true)
    public String exportToHtml(User currentUser) {
        List<Bookmark> bookmarks;
        if (currentUser.getRole() == Role.ADMIN) {
            bookmarks = bookmarkRepository.findAllWithTagsAndFolder();
        } else {
            bookmarks = bookmarkRepository.findByUserId(currentUser.getId(), Pageable.unpaged()).getContent();
        }

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE NETSCAPE-Bookmark-file-1>\n");
        html.append("<!-- This is an automatically generated file. -->\n");
        html.append("<META HTTP-EQUIV=\"Content-Type\" CONTENT=\"text/html; charset=UTF-8\">\n");
        html.append("<TITLE>LinkVault Bookmarks</TITLE>\n");
        html.append("<H1>LinkVault Bookmarks</H1>\n");
        html.append("<DL><p>\n");

        // Group by folder
        Map<String, List<Bookmark>> grouped = new LinkedHashMap<>();
        grouped.put("", new ArrayList<>());

        for (Bookmark b : bookmarks) {
            String folderPath = folderService.buildFolderPath(b.getFolder());
            grouped.computeIfAbsent(folderPath, k -> new ArrayList<>()).add(b);
        }

        for (Map.Entry<String, List<Bookmark>> entry : grouped.entrySet()) {
            String folder = entry.getKey();
            if (!folder.isEmpty()) {
                html.append("    <DT><H3>").append(escapeHtml(folder)).append("</H3>\n");
                html.append("    <DL><p>\n");
            }

            for (Bookmark b : entry.getValue()) {
                String indent = folder.isEmpty() ? "    " : "        ";
                html.append(indent).append("<DT><A HREF=\"").append(escapeHtml(b.getUrl())).append("\"");
                if (b.getFavicon() != null && !b.getFavicon().isEmpty()) {
                    html.append(" ICON=\"").append(escapeHtml(b.getFavicon())).append("\"");
                }
                if (!b.getTags().isEmpty()) {
                    String tags = b.getTags().stream().map(Tag::getName).collect(Collectors.joining(","));
                    html.append(" TAGS=\"").append(escapeHtml(tags)).append("\"");
                }
                html.append(">").append(escapeHtml(b.getTitle())).append("</A>\n");
                if (b.getDescription() != null && !b.getDescription().isEmpty()) {
                    html.append(indent).append("<DD>").append(escapeHtml(b.getDescription())).append("\n");
                }
            }

            if (!folder.isEmpty()) {
                html.append("    </DL><p>\n");
            }
        }

        html.append("</DL><p>\n");
        return html.toString();
    }

    // --- HTML Import (Netscape bookmark format) ---

    @Transactional
    public int importFromHtml(User currentUser, String html) {
        int imported = 0;

        String[] lines = html.split("\n");
        String currentFolder = null;

        for (String line : lines) {
            String trimmed = line.trim();

            // Detect folder headers: <DT><H3>FolderName</H3>
            if (trimmed.matches("(?i).*<H3[^>]*>(.*?)</H3>.*")) {
                currentFolder = trimmed.replaceAll("(?i).*<H3[^>]*>(.*?)</H3>.*", "$1").trim();
            }

            // Detect bookmark links: <DT><A HREF="url" ...>title</A>
            if (trimmed.matches("(?i).*<A\\s+HREF=\"[^\"]+\"[^>]*>.*</A>.*")) {
                String url = trimmed.replaceAll("(?i).*HREF=\"([^\"]+)\".*", "$1");
                String title = trimmed.replaceAll("(?i).*<A[^>]*>(.*?)</A>.*", "$1");

                if (bookmarkRepository.existsByUrlAndUserId(url, currentUser.getId())) {
                    continue;
                }

                // Parse TAGS attribute if present
                Set<String> tagNames = new HashSet<>();
                if (trimmed.matches("(?i).*TAGS=\"[^\"]+\".*")) {
                    String tags = trimmed.replaceAll("(?i).*TAGS=\"([^\"]+)\".*", "$1");
                    tagNames.addAll(Arrays.asList(tags.split(",")));
                }

                Folder folder = resolveOrCreateFolderPath(currentFolder, currentUser);

                Bookmark bookmark = Bookmark.builder()
                        .title(title)
                        .url(url)
                        .folder(folder)
                        .user(currentUser)
                        .build();

                for (String tagName : tagNames) {
                    String trimmedTag = tagName.trim();
                    if (!trimmedTag.isEmpty()) {
                        Tag tag = tagRepository.findByName(trimmedTag)
                                .orElseGet(() -> tagRepository.save(new Tag(trimmedTag)));
                        bookmark.addTag(tag);
                    }
                }

                bookmarkRepository.save(bookmark);
                imported++;
            }
        }
        return imported;
    }

    private Folder resolveOrCreateFolderPath(String folderPath, User currentUser) {
        if (folderPath == null || folderPath.isEmpty()) return null;

        String[] parts = folderPath.split("/");
        Folder parent = null;

        for (String part : parts) {
            String name = part.trim();
            if (name.isEmpty()) continue;

            List<Folder> candidates;
            if (parent == null) {
                candidates = folderRepository.findRootFolders();
            } else {
                candidates = folderRepository.findByParentIdOrderByDisplayOrderAsc(parent.getId());
            }

            Folder found = candidates.stream()
                    .filter(f -> f.getName().equals(name))
                    .findFirst()
                    .orElse(null);

            if (found == null) {
                Folder newFolder = new Folder(name);
                newFolder.setParent(parent);
                newFolder.setUser(currentUser);
                newFolder.setDisplayOrder(candidates.size());
                found = folderRepository.save(newFolder);
            }
            parent = found;
        }
        return parent;
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
