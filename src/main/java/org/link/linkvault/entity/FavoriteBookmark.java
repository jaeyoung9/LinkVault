package org.link.linkvault.entity;

import javax.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "favorite_bookmarks", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "bookmark_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FavoriteBookmark {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bookmark_id", nullable = false)
    private Bookmark bookmark;

    @Setter
    @Column(nullable = false)
    private int displayOrder;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public FavoriteBookmark(User user, Bookmark bookmark, int displayOrder) {
        this.user = user;
        this.bookmark = bookmark;
        this.displayOrder = displayOrder;
    }
}
