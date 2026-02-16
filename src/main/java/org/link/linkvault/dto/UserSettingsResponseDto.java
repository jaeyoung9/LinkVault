package org.link.linkvault.dto;

import lombok.Builder;
import lombok.Getter;
import org.link.linkvault.entity.ProfileVisibility;
import org.link.linkvault.entity.Theme;
import org.link.linkvault.entity.UserSettings;

@Getter
@Builder
public class UserSettingsResponseDto {
    private Long id;
    private Theme theme;
    private boolean emailNotifications;
    private boolean browserNotifications;
    private ProfileVisibility profileVisibility;
    private boolean showEmail;
    private boolean twoFactorEnabled;

    public static UserSettingsResponseDto from(UserSettings s) {
        return UserSettingsResponseDto.builder()
                .id(s.getId())
                .theme(s.getTheme())
                .emailNotifications(s.isEmailNotifications())
                .browserNotifications(s.isBrowserNotifications())
                .profileVisibility(s.getProfileVisibility())
                .showEmail(s.isShowEmail())
                .twoFactorEnabled(s.isTwoFactorEnabled())
                .build();
    }
}
