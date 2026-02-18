package org.link.linkvault.dto;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Getter
@Setter
public class GuestEventRequestDto {
    @NotBlank
    private String sessionId;
    @NotNull
    private String eventType;
    private String pageUrl;
}
