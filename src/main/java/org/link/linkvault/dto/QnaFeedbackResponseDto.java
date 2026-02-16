package org.link.linkvault.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class QnaFeedbackResponseDto {
    private Long articleId;
    private Boolean userFeedback;
    private int helpfulCount;
    private int notHelpfulCount;
}
