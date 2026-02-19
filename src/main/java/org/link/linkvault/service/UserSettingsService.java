package org.link.linkvault.service;

import lombok.RequiredArgsConstructor;
import org.link.linkvault.dto.UserSettingsResponseDto;
import org.link.linkvault.entity.ProfileVisibility;
import org.link.linkvault.entity.Theme;
import org.link.linkvault.entity.User;
import org.link.linkvault.entity.UserSettings;
import org.link.linkvault.repository.UserRepository;
import org.link.linkvault.repository.UserSettingsRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserSettingsService {

    private final UserSettingsRepository userSettingsRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserSettingsResponseDto getSettings(User user) {
        UserSettings settings = getOrCreateSettings(user);
        return UserSettingsResponseDto.from(settings);
    }

    @Transactional
    public UserSettings getOrCreateSettings(User user) {
        return userSettingsRepository.findByUserId(user.getId())
                .orElseGet(() -> userSettingsRepository.save(
                        UserSettings.builder().user(user).theme(Theme.DARK).build()));
    }

    @Transactional
    public UserSettingsResponseDto updateTheme(User user, Theme theme) {
        UserSettings settings = getOrCreateSettings(user);
        settings.updateTheme(theme);
        return UserSettingsResponseDto.from(settings);
    }

    @Transactional
    public UserSettingsResponseDto updateNotifications(User user, boolean email, boolean browser) {
        UserSettings settings = getOrCreateSettings(user);
        settings.updateNotifications(email, browser);
        return UserSettingsResponseDto.from(settings);
    }

    @Transactional
    public UserSettingsResponseDto updatePrivacy(User user, ProfileVisibility visibility, boolean showEmail) {
        UserSettings settings = getOrCreateSettings(user);
        settings.updatePrivacy(visibility, showEmail);
        return UserSettingsResponseDto.from(settings);
    }

    @Transactional
    public void changePassword(User user, String currentPassword, String newPassword, String confirmPassword) {
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }
        if (!newPassword.equals(confirmPassword)) {
            throw new IllegalArgumentException("New password and confirmation do not match");
        }
        if (newPassword.length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters");
        }
        user.updatePassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Transactional
    public void changeEmail(User user, String newEmail) {
        if (newEmail == null || newEmail.isBlank()) {
            throw new IllegalArgumentException("Email cannot be empty");
        }
        userRepository.findByEmail(newEmail).ifPresent(existing -> {
            if (!existing.getId().equals(user.getId())) {
                throw new IllegalArgumentException("Email already in use");
            }
        });
        user.updateProfile(newEmail);
        userRepository.save(user);
    }

    @Transactional
    public void completeGuidelines(User user) {
        UserSettings settings = getOrCreateSettings(user);
        settings.completeGuidelines();
    }

    public boolean isGuidelinesCompleted(User user) {
        UserSettings settings = getOrCreateSettings(user);
        return settings.isGuidelinesCompleted();
    }

    public Theme getThemeForUser(Long userId) {
        return userSettingsRepository.findThemeByUserId(userId).orElse(Theme.DARK);
    }
}
