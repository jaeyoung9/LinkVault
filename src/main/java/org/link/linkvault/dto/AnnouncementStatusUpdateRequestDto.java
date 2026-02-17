package org.link.linkvault.dto;

import lombok.Getter;
import lombok.Setter;
import org.link.linkvault.entity.AnnouncementStatus;

import javax.validation.constraints.NotNull;

@Getter
@Setter
public class AnnouncementStatusUpdateRequestDto {
    @NotNull(message = "Status is required")
    private AnnouncementStatus status;
}
