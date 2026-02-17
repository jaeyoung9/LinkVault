package org.link.linkvault.entity;

import javax.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "bookmarks", indexes = {
        @Index(name = "idx_bookmark_url", columnList = "url"),
        @Index(name = "idx_bookmark_title", columnList = "title"),
        @Index(name = "idx_bookmark_folder", columnList = "folder_id"),
        @Index(name = "idx_bookmark_user", columnList = "user_id"),
        @Index(name = "idx_bookmark_access_count", columnList = "accessCount"),
        @Index(name = "idx_bookmark_created_at", columnList = "createdAt"),
        @Index(name = "idx_bookmark_location", columnList = "latitude, longitude")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Bookmark {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(length = 2048)
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

    private Double latitude;

    private Double longitude;

    @Column(length = 300)
    private String address;

    @Lob
    private String caption;

    @Column(length = 10)
    private String mapEmoji;

    @OneToMany(mappedBy = "bookmark", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    private List<PostPhoto> photos = new ArrayList<>();

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
            name = "bookmark_tags",
            joinColumns = @JoinColumn(name = "bookmark_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private Set<Tag> tags = new HashSet<>();

    @Column(nullable = false)
    private boolean deleted = false;

    @Column(name = "is_private", nullable = false)
    private boolean privatePost = false;

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
    public Bookmark(String title, String url, String description, String favicon, Folder folder, User user,
                    Double latitude, Double longitude, String address, String caption, String mapEmoji, boolean privatePost) {
        this.title = title;
        this.url = url;
        this.description = description;
        this.favicon = favicon;
        this.folder = folder;
        this.user = user;
        this.latitude = latitude;
        this.longitude = longitude;
        this.address = address;
        this.caption = caption;
        this.mapEmoji = mapEmoji;
        this.privatePost = privatePost;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public void update(String title, String url, String description) {
        this.title = title;
        this.url = url;
        this.description = description;
    }

    public void update(String title, String url, String description,
                       Double latitude, Double longitude, String address, String caption, String mapEmoji) {
        this.title = title;
        this.url = url;
        this.description = description;
        this.latitude = latitude;
        this.longitude = longitude;
        this.address = address;
        this.caption = caption;
        this.mapEmoji = mapEmoji;
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

    public void softDelete() {
        this.deleted = true;
    }

    public void restore() {
        this.deleted = false;
    }

    public void addPhoto(PostPhoto photo) {
        photos.add(photo);
        photo.setBookmark(this);
    }

    public void removePhoto(PostPhoto photo) {
        photos.remove(photo);
        photo.setBookmark(null);
    }

    public void setPrivatePost(boolean privatePost) { this.privatePost = privatePost; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public void setAddress(String address) { this.address = address; }
    public void setCaption(String caption) { this.caption = caption; }
    public void setMapEmoji(String mapEmoji) { this.mapEmoji = mapEmoji; }
}
