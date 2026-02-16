package org.link.linkvault.entity;

import javax.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "comments", indexes = {
        @Index(name = "idx_comment_bookmark", columnList = "bookmark_id"),
        @Index(name = "idx_comment_user", columnList = "user_id"),
        @Index(name = "idx_comment_parent", columnList = "parent_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 2000)
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bookmark_id", nullable = false)
    private Bookmark bookmark;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Comment parent;

    @OneToMany(mappedBy = "parent")
    @OrderBy("createdAt ASC")
    private List<Comment> replies = new ArrayList<>();

    @Column(length = 2000)
    private String originalContent;

    @Column(nullable = false)
    private boolean deleted = false;

    @Column(nullable = false)
    private boolean edited = false;

    @Column(nullable = false)
    private int depth = 0;

    @Column(nullable = false)
    private int likeCount = 0;

    @Column(nullable = false)
    private int dislikeCount = 0;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @Builder
    public Comment(String content, User user, Bookmark bookmark, Comment parent, int depth) {
        this.content = content;
        this.user = user;
        this.bookmark = bookmark;
        this.parent = parent;
        this.depth = depth;
    }

    public void updateContent(String content) {
        this.content = content;
        this.edited = true;
        this.updatedAt = LocalDateTime.now();
    }

    public void softDelete() {
        this.originalContent = this.content;
        this.deleted = true;
        this.content = "[deleted]";
    }

    public void restore() {
        if (this.originalContent != null) {
            this.content = this.originalContent;
            this.originalContent = null;
        }
        this.deleted = false;
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
