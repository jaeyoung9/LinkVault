package org.link.linkvault.repository;

import org.link.linkvault.entity.Theme;
import org.link.linkvault.entity.UserSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserSettingsRepository extends JpaRepository<UserSettings, Long> {

    Optional<UserSettings> findByUserId(Long userId);

    @Query("SELECT us.theme FROM UserSettings us WHERE us.user.id = :userId")
    Optional<Theme> findThemeByUserId(@Param("userId") Long userId);
}
