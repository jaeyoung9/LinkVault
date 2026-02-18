package org.link.linkvault.entity;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "transparency_reports", indexes = {
        @Index(name = "idx_transparency_published", columnList = "published")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TransparencyReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 50)
    private String period;

    @Column(nullable = false, columnDefinition = "CLOB")
    private String content;

    @Column(nullable = false)
    private int totalDonationsCents;

    @Column(nullable = false)
    private int totalPassRevenueCents;

    @Column(nullable = false)
    private boolean published = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime publishedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    @Builder
    public TransparencyReport(String title, String period, String content,
                              int totalDonationsCents, int totalPassRevenueCents, User createdBy) {
        this.title = title;
        this.period = period;
        this.content = content;
        this.totalDonationsCents = totalDonationsCents;
        this.totalPassRevenueCents = totalPassRevenueCents;
        this.createdBy = createdBy;
    }

    public void update(String title, String period, String content,
                       int totalDonationsCents, int totalPassRevenueCents) {
        this.title = title;
        this.period = period;
        this.content = content;
        this.totalDonationsCents = totalDonationsCents;
        this.totalPassRevenueCents = totalPassRevenueCents;
    }

    public void publish() {
        this.published = true;
        this.publishedAt = LocalDateTime.now();
    }

    public void unpublish() {
        this.published = false;
        this.publishedAt = null;
    }
}
