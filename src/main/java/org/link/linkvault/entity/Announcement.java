package org.link.linkvault.entity;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

    @Column(nullable = false)
    private boolean enableComments = false;

    @Column(nullable = false)
    private boolean enableVoting = false;

    @Column(nullable = false)
    private int likeCount = 0;

    @Column(nullable = false)
    private int dislikeCount = 0;

    @OneToMany(mappedBy = "announcement", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    private List<AnnouncementPollOption> pollOptions = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @Builder
    public Announcement(String title, String content, AnnouncementPriority priority,
                        AnnouncementStatus status, Role targetRole, LocalDateTime startAt,
                        LocalDateTime endAt, boolean pinned, boolean enableComments,
                        boolean enableVoting, User createdBy) {
        this.title = title;
        this.content = content;
        this.priority = priority != null ? priority : AnnouncementPriority.INFO;
        this.status = status != null ? status : AnnouncementStatus.DRAFT;
        this.targetRole = targetRole;
        this.startAt = startAt;
        this.endAt = endAt;
        this.pinned = pinned;
        this.enableComments = enableComments;
        this.enableVoting = enableVoting;
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
                       Role targetRole, LocalDateTime startAt, LocalDateTime endAt,
                       boolean pinned, boolean enableComments, boolean enableVoting) {
        this.title = title;
        this.content = content;
        this.priority = priority;
        this.targetRole = targetRole;
        this.startAt = startAt;
        this.endAt = endAt;
        this.pinned = pinned;
        this.enableComments = enableComments;
        this.enableVoting = enableVoting;
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

    public void incrementLikeCount() {
        this.likeCount++;
    }

    public void decrementLikeCount() {
        if (this.likeCount > 0) this.likeCount--;
    }

    public void incrementDislikeCount() {
        this.dislikeCount++;
    }

    public void decrementDislikeCount() {
        if (this.dislikeCount > 0) this.dislikeCount--;
    }
}
