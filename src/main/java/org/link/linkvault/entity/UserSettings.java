package org.link.linkvault.entity;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_settings")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Theme theme = Theme.DARK;

    @Column(nullable = false)
    private boolean emailNotifications = true;

    @Column(nullable = false)
    private boolean browserNotifications = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ProfileVisibility profileVisibility = ProfileVisibility.PUBLIC;

    @Column(nullable = false)
    private boolean showEmail = false;

    @Column(nullable = false)
    private boolean twoFactorEnabled = false;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @Builder
    public UserSettings(User user, Theme theme) {
        this.user = user;
        this.theme = theme != null ? theme : Theme.DARK;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void updateTheme(Theme theme) {
        this.theme = theme;
    }

    public void updateNotifications(boolean email, boolean browser) {
        this.emailNotifications = email;
        this.browserNotifications = browser;
    }

    public void updatePrivacy(ProfileVisibility visibility, boolean showEmail) {
        this.profileVisibility = visibility;
        this.showEmail = showEmail;
    }
}
