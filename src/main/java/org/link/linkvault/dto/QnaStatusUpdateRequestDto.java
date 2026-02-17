package org.link.linkvault.dto;

import lombok.Getter;
import lombok.Setter;
import org.link.linkvault.entity.QnaStatus;

import javax.validation.constraints.NotNull;

@Getter
@Setter
public class QnaStatusUpdateRequestDto {
    @NotNull(message = "Status is required")
    private QnaStatus status;
}
