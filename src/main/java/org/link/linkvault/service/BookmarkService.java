package org.link.linkvault.service;

import lombok.RequiredArgsConstructor;
import org.link.linkvault.dto.BookmarkRequestDto;
import org.link.linkvault.dto.BookmarkResponseDto;
import org.link.linkvault.entity.Bookmark;
import org.link.linkvault.entity.Tag;
import org.link.linkvault.repository.BookmarkRepository;
import org.link.linkvault.repository.TagRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookmarkService {

    private final BookmarkRepository bookmarkRepository;
    private final TagRepository tagRepository;

    public List<BookmarkResponseDto> findAll() {
        return bookmarkRepository.findAll().stream()
                .map(BookmarkResponseDto::from)
                .collect(Collectors.toList());
    }

    public BookmarkResponseDto findById(Long id) {
        Bookmark bookmark = bookmarkRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Bookmark not found with id: " + id));
        return BookmarkResponseDto.from(bookmark);
    }

    @Transactional
    public BookmarkResponseDto create(BookmarkRequestDto requestDto) {
        Bookmark bookmark = Bookmark.builder()
                .title(requestDto.getTitle())
                .url(requestDto.getUrl())
                .description(requestDto.getDescription())
                .build();

        assignTags(bookmark, requestDto.getTagNames());

        Bookmark saved = bookmarkRepository.save(bookmark);
        return BookmarkResponseDto.from(saved);
    }

    @Transactional
    public BookmarkResponseDto update(Long id, BookmarkRequestDto requestDto) {
        Bookmark bookmark = bookmarkRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Bookmark not found with id: " + id));

        bookmark.update(requestDto.getTitle(), requestDto.getUrl(), requestDto.getDescription());

        // Clear existing tags and reassign
        new HashSet<>(bookmark.getTags()).forEach(bookmark::removeTag);
        assignTags(bookmark, requestDto.getTagNames());

        return BookmarkResponseDto.from(bookmark);
    }

    @Transactional
    public void delete(Long id) {
        Bookmark bookmark = bookmarkRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Bookmark not found with id: " + id));
        bookmarkRepository.delete(bookmark);
    }

    public List<BookmarkResponseDto> searchByKeyword(String keyword) {
        return bookmarkRepository.searchByKeyword(keyword).stream()
                .map(BookmarkResponseDto::from)
                .collect(Collectors.toList());
    }

    public List<BookmarkResponseDto> findByTagName(String tagName) {
        return bookmarkRepository.findByTagName(tagName).stream()
                .map(BookmarkResponseDto::from)
                .collect(Collectors.toList());
    }

    private void assignTags(Bookmark bookmark, Set<String> tagNames) {
        if (tagNames == null || tagNames.isEmpty()) {
            return;
        }
        for (String tagName : tagNames) {
            Tag tag = tagRepository.findByName(tagName)
                    .orElseGet(() -> tagRepository.save(new Tag(tagName)));
            bookmark.addTag(tag);
        }
    }
}
