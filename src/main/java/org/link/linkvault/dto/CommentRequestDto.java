package org.link.linkvault.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CommentRequestDto {

    @NotBlank(message = "Comment content is required")
    @Size(max = 2000, message = "Comment must be 2000 characters or less")
    private String content;

    private Long bookmarkId;

    private Long announcementId;

    private Long parentId;
}
