package org.link.linkvault.dto;

import lombok.Getter;
import lombok.Setter;
import org.link.linkvault.entity.QnaStatus;

import javax.validation.constraints.NotBlank;

@Getter
@Setter
public class QnaArticleRequestDto {
    @NotBlank
    private String question;

    @NotBlank
    private String answer;

    @NotBlank
    private String category;

    private String tags;
    private String relatedLinks;
    private Integer displayOrder;
    private QnaStatus status;
}
