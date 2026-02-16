package org.link.linkvault.entity;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "announcements")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Announcement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(nullable = false, columnDefinition = "CLOB")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AnnouncementPriority priority = AnnouncementPriority.INFO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AnnouncementStatus status = AnnouncementStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Role targetRole;

    private LocalDateTime startAt;

    private LocalDateTime endAt;

    @Column(nullable = false)
    private boolean pinned = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @Builder
    public Announcement(String title, String content, AnnouncementPriority priority,
                        AnnouncementStatus status, Role targetRole, LocalDateTime startAt,
                        LocalDateTime endAt, boolean pinned, User createdBy) {
        this.title = title;
        this.content = content;
        this.priority = priority != null ? priority : AnnouncementPriority.INFO;
        this.status = status != null ? status : AnnouncementStatus.DRAFT;
        this.targetRole = targetRole;
        this.startAt = startAt;
        this.endAt = endAt;
        this.pinned = pinned;
        this.createdBy = createdBy;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void update(String title, String content, AnnouncementPriority priority,
                       Role targetRole, LocalDateTime startAt, LocalDateTime endAt, boolean pinned) {
        this.title = title;
        this.content = content;
        this.priority = priority;
        this.targetRole = targetRole;
        this.startAt = startAt;
        this.endAt = endAt;
        this.pinned = pinned;
    }

    public void updateStatus(AnnouncementStatus status) {
        this.status = status;
    }

    public boolean shouldPublish() {
        return status == AnnouncementStatus.SCHEDULED && startAt != null && !startAt.isAfter(LocalDateTime.now());
    }

    public boolean shouldArchive() {
        return status == AnnouncementStatus.PUBLISHED && endAt != null && !endAt.isAfter(LocalDateTime.now());
    }
}
