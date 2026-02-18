package org.link.linkvault.entity;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "guest_events", indexes = {
        @Index(name = "idx_guestevent_session", columnList = "sessionId"),
        @Index(name = "idx_guestevent_type", columnList = "eventType"),
        @Index(name = "idx_guestevent_created", columnList = "createdAt")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GuestEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String sessionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private GuestEventType eventType;

    @Column(length = 500)
    private String pageUrl;

    @Column(length = 500)
    private String referrer;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @Builder
    public GuestEvent(String sessionId, GuestEventType eventType, String pageUrl, String referrer) {
        this.sessionId = sessionId;
        this.eventType = eventType;
        this.pageUrl = pageUrl;
        this.referrer = referrer;
    }
}
