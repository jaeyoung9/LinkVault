package org.link.linkvault.service;

import lombok.RequiredArgsConstructor;
import org.link.linkvault.entity.SystemSettings;
import org.link.linkvault.repository.SystemSettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SystemSettingsService {

    private final SystemSettingsRepository systemSettingsRepository;
    private final AuditLogService auditLogService;

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
        settings.updateValue(value);
        SystemSettings saved = systemSettingsRepository.save(settings);
        auditLogService.log(actorUsername, AuditActionCodes.SETTINGS_UPDATE, "SystemSettings", null,
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
}
