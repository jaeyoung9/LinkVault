package org.link.linkvault.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotBlank;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FolderRequestDto {

    @NotBlank(message = "Folder name is required")
    private String name;

    private Long parentId;
}
