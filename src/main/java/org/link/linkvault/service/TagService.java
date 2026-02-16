package org.link.linkvault.service;

import lombok.RequiredArgsConstructor;
import org.link.linkvault.dto.TagResponseDto;
import org.link.linkvault.entity.Bookmark;
import org.link.linkvault.entity.Tag;
import org.link.linkvault.exception.ResourceNotFoundException;
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
public class TagService {

    private final TagRepository tagRepository;

    public List<TagResponseDto> findAll() {
        return tagRepository.findAllWithBookmarks().stream()
                .map(TagResponseDto::from)
                .collect(Collectors.toList());
    }

    public TagResponseDto findById(Long id) {
        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tag not found with id: " + id));
        return TagResponseDto.from(tag);
    }

    @Transactional
    public TagResponseDto create(String name) {
        if (tagRepository.existsByName(name)) {
            throw new IllegalArgumentException("Tag already exists with name: " + name);
        }
        Tag tag = tagRepository.save(new Tag(name));
        return TagResponseDto.from(tag);
    }

    @Transactional
    public void delete(Long id) {
        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tag not found with id: " + id));
        // Remove tag from all bookmarks first
        for (Bookmark bookmark : new HashSet<>(tag.getBookmarks())) {
            bookmark.removeTag(tag);
        }
        tagRepository.delete(tag);
    }

    @Transactional
    public void mergeTags(Set<Long> sourceTagIds, String targetTagName) {
        Tag targetTag = tagRepository.findByName(targetTagName)
                .orElseGet(() -> tagRepository.save(new Tag(targetTagName)));

        for (Long sourceId : sourceTagIds) {
            Tag sourceTag = tagRepository.findById(sourceId)
                    .orElseThrow(() -> new ResourceNotFoundException("Tag not found with id: " + sourceId));

            if (sourceTag.getId().equals(targetTag.getId())) {
                continue;
            }

            // Move bookmarks from source to target
            for (Bookmark bookmark : new HashSet<>(sourceTag.getBookmarks())) {
                bookmark.removeTag(sourceTag);
                bookmark.addTag(targetTag);
            }

            tagRepository.delete(sourceTag);
        }
    }

    @Transactional
    public int deleteUnusedTags() {
        List<Tag> unused = tagRepository.findUnusedTags();
        tagRepository.deleteAll(unused);
        return unused.size();
    }
}
