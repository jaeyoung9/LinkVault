package org.link.linkvault.entity;

import javax.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "post_photos")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostPhoto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bookmark_id", nullable = false)
    private Bookmark bookmark;

    @Column(nullable = false)
    private String storagePath;

    @Column(nullable = false)
    private String originalFilename;

    private Long fileSize;

    private String contentType;

    @Column(nullable = false)
    private int displayOrder;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @Builder
    public PostPhoto(Bookmark bookmark, String storagePath, String originalFilename,
                     Long fileSize, String contentType, int displayOrder) {
        this.bookmark = bookmark;
        this.storagePath = storagePath;
        this.originalFilename = originalFilename;
        this.fileSize = fileSize;
        this.contentType = contentType;
        this.displayOrder = displayOrder;
    }

    public void setBookmark(Bookmark bookmark) {
        this.bookmark = bookmark;
    }

    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }
}
