package org.link.linkvault.entity;

import javax.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "announcement_votes", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "announcement_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AnnouncementVote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "announcement_id", nullable = false)
    private Announcement announcement;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private VoteType voteType;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public AnnouncementVote(User user, Announcement announcement, VoteType voteType) {
        this.user = user;
        this.announcement = announcement;
        this.voteType = voteType;
    }

    public void changeVoteType(VoteType voteType) {
        this.voteType = voteType;
    }
}
