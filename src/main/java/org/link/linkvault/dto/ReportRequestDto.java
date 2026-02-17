package org.link.linkvault.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.link.linkvault.entity.ReportTargetType;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReportRequestDto {

    @NotNull(message = "Target type is required")
    private ReportTargetType targetType;

    @NotNull(message = "Target ID is required")
    private Long targetId;

    @NotBlank(message = "Reason is required")
    @Size(max = 1000, message = "Reason must be 1000 characters or less")
    private String reason;
}
