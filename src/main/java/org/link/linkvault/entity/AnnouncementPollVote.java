package org.link.linkvault.entity;

import javax.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "announcement_poll_votes", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "announcement_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AnnouncementPollVote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "announcement_id", nullable = false)
    private Announcement announcement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "poll_option_id", nullable = false)
    private AnnouncementPollOption pollOption;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public AnnouncementPollVote(User user, Announcement announcement, AnnouncementPollOption pollOption) {
        this.user = user;
        this.announcement = announcement;
        this.pollOption = pollOption;
    }

    public void changePollOption(AnnouncementPollOption newOption) {
        this.pollOption = newOption;
    }
}
