package org.link.linkvault.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TagMergeRequestDto {

    private Set<Long> sourceTagIds;
    private String targetTagName;
}
