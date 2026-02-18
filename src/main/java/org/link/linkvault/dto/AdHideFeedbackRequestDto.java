package org.link.linkvault.dto;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Getter
@Setter
public class AdHideFeedbackRequestDto {
    @NotBlank
    private String adUnitId;
    @NotNull
    private String reason;
    private String sessionId;
}
