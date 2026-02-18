package org.link.linkvault.entity;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ad_hide_feedback", indexes = {
        @Index(name = "idx_adhide_session", columnList = "sessionId")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdHideFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(length = 100)
    private String sessionId;

    @Column(length = 100)
    private String adUnitId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AdHideReason reason;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @Builder
    public AdHideFeedback(User user, String sessionId, String adUnitId, AdHideReason reason) {
        this.user = user;
        this.sessionId = sessionId;
        this.adUnitId = adUnitId;
        this.reason = reason;
    }
}
