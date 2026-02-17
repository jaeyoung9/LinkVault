package org.link.linkvault.entity;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "system_settings", indexes = {
        @Index(name = "idx_system_settings_category", columnList = "category")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SystemSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "setting_key", nullable = false, unique = true, length = 100)
    private String settingKey;

    @Column(name = "setting_value", length = 1000)
    private String settingValue;

    @Column(length = 200)
    private String description;

    @Column(length = 50)
    private String category;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @Builder
    public SystemSettings(String settingKey, String settingValue, String description, String category) {
        this.settingKey = settingKey;
        this.settingValue = settingValue;
        this.description = description;
        this.category = category;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void updateValue(String value) {
        this.settingValue = value;
    }
}
