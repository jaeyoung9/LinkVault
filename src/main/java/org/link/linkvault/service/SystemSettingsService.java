package org.link.linkvault.service;

import lombok.RequiredArgsConstructor;
import org.link.linkvault.entity.SystemSettings;
import org.link.linkvault.repository.SystemSettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SystemSettingsService {

    private final SystemSettingsRepository systemSettingsRepository;
    private final AuditLogService auditLogService;

    private static final Set<String> AUDIT_POLICY_KEYS = new HashSet<>(Arrays.asList(
            "audit.retention.enabled", "audit.retention.days",
            "audit.delete.mode", "audit.masking.level"));

    private static final Set<String> LOCKOUT_POLICY_KEYS = new HashSet<>(Arrays.asList(
            "security.lockout-threshold", "security.lockout-duration-minutes"));

    public List<SystemSettings> findAll() {
        return systemSettingsRepository.findAllByOrderByCategoryAscSettingKeyAsc();
    }

    public List<SystemSettings> findByCategory(String category) {
        return systemSettingsRepository.findByCategoryOrderBySettingKeyAsc(category);
    }

    public Optional<String> getValue(String key) {
        return systemSettingsRepository.findBySettingKey(key)
                .map(SystemSettings::getSettingValue);
    }

    @Transactional
    public SystemSettings updateValue(String key, String value, String actorUsername) {
        SystemSettings settings = systemSettingsRepository.findBySettingKey(key)
                .orElseThrow(() -> new IllegalArgumentException("Setting not found: " + key));

        if (AUDIT_POLICY_KEYS.contains(key)) {
            validateAuditSetting(key, value);
        }
        if (LOCKOUT_POLICY_KEYS.contains(key)) {
            validateLockoutSetting(key, value);
        }

        settings.updateValue(value);
        SystemSettings saved = systemSettingsRepository.save(settings);

        String actionCode;
        if (AUDIT_POLICY_KEYS.contains(key)) {
            actionCode = AuditActionCodes.AUDIT_POLICY_UPDATE;
        } else if (LOCKOUT_POLICY_KEYS.contains(key)) {
            actionCode = AuditActionCodes.LOCKOUT_POLICY_UPDATE;
        } else {
            actionCode = AuditActionCodes.SETTINGS_UPDATE;
        }
        auditLogService.log(actorUsername, actionCode, "SystemSettings", null,
                AuditDetailFormatter.format("key", key));
        return saved;
    }

    @Transactional
    public SystemSettings createOrUpdate(String key, String value, String description, String category) {
        Optional<SystemSettings> existing = systemSettingsRepository.findBySettingKey(key);
        if (existing.isPresent()) {
            SystemSettings settings = existing.get();
            settings.updateValue(value);
            return systemSettingsRepository.save(settings);
        }
        SystemSettings settings = SystemSettings.builder()
                .settingKey(key)
                .settingValue(value)
                .description(description)
                .category(category)
                .build();
        return systemSettingsRepository.save(settings);
    }

    private void validateLockoutSetting(String key, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Value is required for setting: " + key);
        }
        try {
            int val = Integer.parseInt(value);
            if ("security.lockout-threshold".equals(key) && (val < 1 || val > 20)) {
                throw new IllegalArgumentException("security.lockout-threshold must be between 1 and 20");
            }
            if ("security.lockout-duration-minutes".equals(key) && (val < 30 || val > 1440)) {
                throw new IllegalArgumentException("security.lockout-duration-minutes must be between 30 and 1440");
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(key + " must be an integer");
        }
    }

    private void validateAuditSetting(String key, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Value is required for setting: " + key);
        }
        switch (key) {
            case "audit.retention.enabled":
                if (!"true".equals(value) && !"false".equals(value)) {
                    throw new IllegalArgumentException("audit.retention.enabled must be 'true' or 'false'");
                }
                break;
            case "audit.retention.days":
                try {
                    int days = Integer.parseInt(value);
                    if (days < 30 || days > 3650) {
                        throw new IllegalArgumentException("audit.retention.days must be between 30 and 3650");
                    }
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("audit.retention.days must be an integer");
                }
                break;
            case "audit.delete.mode":
                if (!"SOFT".equals(value) && !"HARD".equals(value)) {
                    throw new IllegalArgumentException("audit.delete.mode must be 'SOFT' or 'HARD'");
                }
                break;
            case "audit.masking.level":
                if (!"NONE".equals(value) && !"BASIC".equals(value) && !"STRICT".equals(value)) {
                    throw new IllegalArgumentException("audit.masking.level must be 'NONE', 'BASIC', or 'STRICT'");
                }
                break;
            default:
                break;
        }
    }
}
