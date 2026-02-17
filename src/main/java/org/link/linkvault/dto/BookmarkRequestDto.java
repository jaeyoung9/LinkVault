package org.link.linkvault.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookmarkRequestDto {

    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title must be 255 characters or less")
    private String title;

    @Size(max = 2048, message = "URL must be 2048 characters or less")
    private String url;

    @Size(max = 1000, message = "Description must be 1000 characters or less")
    private String description;

    private Set<String> tagNames;

    private Long folderId;

    private Double latitude;

    private Double longitude;

    @Size(max = 300, message = "Address must be 300 characters or less")
    private String address;

    private String caption;

    @Size(max = 10, message = "Map emoji must be 10 characters or less")
    private String mapEmoji;

    private Boolean privatePost;
}
