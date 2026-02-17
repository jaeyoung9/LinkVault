package org.link.linkvault.repository;

import org.link.linkvault.entity.SystemSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SystemSettingsRepository extends JpaRepository<SystemSettings, Long> {

    Optional<SystemSettings> findBySettingKey(String settingKey);

    List<SystemSettings> findByCategoryOrderBySettingKeyAsc(String category);

    List<SystemSettings> findAllByOrderByCategoryAscSettingKeyAsc();
}
