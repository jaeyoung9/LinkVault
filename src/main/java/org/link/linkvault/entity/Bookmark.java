package org.link.linkvault.entity;

import javax.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "bookmarks", indexes = {
        @Index(name = "idx_bookmark_url", columnList = "url"),
        @Index(name = "idx_bookmark_title", columnList = "title"),
        @Index(name = "idx_bookmark_folder", columnList = "folder_id"),
        @Index(name = "idx_bookmark_user", columnList = "user_id"),
        @Index(name = "idx_bookmark_access_count", columnList = "accessCount"),
        @Index(name = "idx_bookmark_created_at", columnList = "createdAt")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Bookmark {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 2048)
    private String url;

    @Column(length = 1000)
    private String description;

    @Column(length = 2048)
    private String favicon;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "folder_id")
    private Folder folder;

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
            name = "bookmark_tags",
            joinColumns = @JoinColumn(name = "bookmark_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private Set<Tag> tags = new HashSet<>();

    @Column(nullable = false)
    private int accessCount = 0;

    @Column(nullable = false)
    private int commentCount = 0;

    private LocalDateTime lastAccessedAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    @Builder
    public Bookmark(String title, String url, String description, String favicon, Folder folder, User user) {
        this.title = title;
        this.url = url;
        this.description = description;
        this.favicon = favicon;
        this.folder = folder;
        this.user = user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public void update(String title, String url, String description) {
        this.title = title;
        this.url = url;
        this.description = description;
    }

    public void setFavicon(String favicon) {
        this.favicon = favicon;
    }

    public void setFolder(Folder folder) {
        this.folder = folder;
    }

    public void recordAccess() {
        this.accessCount++;
        this.lastAccessedAt = LocalDateTime.now();
    }

    public void addTag(Tag tag) {
        this.tags.add(tag);
        tag.getBookmarks().add(this);
    }

    public void removeTag(Tag tag) {
        this.tags.remove(tag);
        tag.getBookmarks().remove(this);
    }

    public void incrementCommentCount() {
        this.commentCount++;
    }

    public void decrementCommentCount() {
        if (this.commentCount > 0) this.commentCount--;
    }
}
