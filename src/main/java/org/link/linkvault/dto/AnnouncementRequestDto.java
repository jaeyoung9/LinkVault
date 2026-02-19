package org.link.linkvault.dto;

import lombok.Getter;
import lombok.Setter;
import org.link.linkvault.entity.AnnouncementPriority;
import org.link.linkvault.entity.AnnouncementStatus;
import org.link.linkvault.entity.Role;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class AnnouncementRequestDto {
    @NotBlank
    private String title;

    @NotBlank
    private String content;

    @NotNull
    private AnnouncementPriority priority;

    private AnnouncementStatus status;
    private Role targetRole;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private boolean pinned;
    private boolean enableComments;
    private boolean enableVoting;
    private List<String> pollOptions;
}
