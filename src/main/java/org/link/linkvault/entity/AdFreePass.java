package org.link.linkvault.entity;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ad_free_passes", indexes = {
        @Index(name = "idx_adfreepass_user", columnList = "user_id"),
        @Index(name = "idx_adfreepass_expires", columnList = "expiresAt")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdFreePass {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AdFreePassType type;

    @Column(nullable = false)
    private LocalDateTime startsAt;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(length = 200)
    private String stripePaymentIntentId;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false)
    private boolean refunded = false;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @Builder
    public AdFreePass(User user, AdFreePassType type, LocalDateTime startsAt,
                      LocalDateTime expiresAt, String stripePaymentIntentId) {
        this.user = user;
        this.type = type;
        this.startsAt = startsAt;
        this.expiresAt = expiresAt;
        this.stripePaymentIntentId = stripePaymentIntentId;
    }

    public boolean isCurrentlyActive() {
        LocalDateTime now = LocalDateTime.now();
        return active && !refunded && now.isAfter(startsAt) && now.isBefore(expiresAt);
    }

    public void markRefunded() {
        this.refunded = true;
        this.active = false;
    }

    public void deactivate() {
        this.active = false;
    }
}
