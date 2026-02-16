package org.link.linkvault.entity;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "announcement_reads", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "announcement_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AnnouncementRead {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "announcement_id", nullable = false)
    private Announcement announcement;

    private LocalDateTime readAt;

    private LocalDateTime acknowledgedAt;

    public AnnouncementRead(User user, Announcement announcement) {
        this.user = user;
        this.announcement = announcement;
        this.readAt = LocalDateTime.now();
    }

    public void acknowledge() {
        this.acknowledgedAt = LocalDateTime.now();
    }
}
