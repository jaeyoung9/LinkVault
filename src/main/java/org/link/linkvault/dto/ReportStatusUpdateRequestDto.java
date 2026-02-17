package org.link.linkvault.dto;

import lombok.Getter;
import lombok.Setter;
import org.link.linkvault.entity.ReportStatus;

import javax.validation.constraints.NotNull;

@Getter
@Setter
public class ReportStatusUpdateRequestDto {
    @NotNull(message = "Status is required")
    private ReportStatus status;
}
