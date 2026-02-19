package org.link.linkvault.entity;

import javax.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "guideline_steps", indexes = {
        @Index(name = "idx_gs_screen", columnList = "screen"),
        @Index(name = "idx_gs_screen_order", columnList = "screen, displayOrder")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GuidelineStep {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30)
    private String screen;

    @Column(nullable = false, length = 50)
    private String targetElement;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 1000)
    private String content;

    @Column(nullable = false)
    private int displayOrder = 0;

    @Column(nullable = false)
    private boolean enabled = true;

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
    public GuidelineStep(String screen, String targetElement, String title,
                         String content, int displayOrder, boolean enabled) {
        this.screen = screen;
        this.targetElement = targetElement;
        this.title = title;
        this.content = content;
        this.displayOrder = displayOrder;
        this.enabled = enabled;
    }

    public void update(String targetElement, String title, String content) {
        this.targetElement = targetElement;
        this.title = title;
        this.content = content;
    }

    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
