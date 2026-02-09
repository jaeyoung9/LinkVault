package org.link.linkvault.service;

import lombok.RequiredArgsConstructor;
import org.link.linkvault.dto.TagResponseDto;
import org.link.linkvault.entity.Tag;
import org.link.linkvault.repository.TagRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TagService {

    private final TagRepository tagRepository;

    public List<TagResponseDto> findAll() {
        return tagRepository.findAll().stream()
                .map(TagResponseDto::from)
                .collect(Collectors.toList());
    }

    public TagResponseDto findById(Long id) {
        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tag not found with id: " + id));
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
                .orElseThrow(() -> new IllegalArgumentException("Tag not found with id: " + id));
        if (!tag.getBookmarks().isEmpty()) {
            throw new IllegalStateException("Cannot delete tag that is still assigned to bookmarks");
        }
        tagRepository.delete(tag);
    }
}
