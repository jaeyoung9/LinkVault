package org.link.linkvault.entity;

import javax.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "announcement_poll_options")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AnnouncementPollOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "announcement_id", nullable = false)
    private Announcement announcement;

    @Column(nullable = false, length = 500)
    private String optionText;

    @Column(nullable = false)
    private int voteCount = 0;

    @Column(nullable = false)
    private int displayOrder = 0;

    public AnnouncementPollOption(Announcement announcement, String optionText, int displayOrder) {
        this.announcement = announcement;
        this.optionText = optionText;
        this.displayOrder = displayOrder;
    }

    public void incrementVoteCount() {
        this.voteCount++;
    }

    public void decrementVoteCount() {
        if (this.voteCount > 0) this.voteCount--;
    }
}
