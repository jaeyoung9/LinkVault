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
@Table(name = "menu_items", indexes = {
        @Index(name = "idx_menu_type", columnList = "menuType"),
        @Index(name = "idx_menu_display_order", columnList = "displayOrder")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MenuItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String label;

    @Column(nullable = false, length = 500)
    private String url;

    @Column(length = 50)
    private String icon;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MenuType menuType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private MenuItem parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    private List<MenuItem> children = new ArrayList<>();

    @Column(nullable = false)
    private int displayOrder = 0;

    @Column(nullable = false)
    private boolean visible = true;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Role requiredRole;

    @Column(length = 50)
    private String requiredPermission;

    @Column(nullable = false)
    private boolean systemItem = false;

    @Column(length = 50)
    private String createdBy;

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
    public MenuItem(String label, String url, String icon, MenuType menuType,
                    MenuItem parent, int displayOrder, boolean visible,
                    Role requiredRole, String requiredPermission,
                    boolean systemItem, String createdBy) {
        this.label = label;
        this.url = url;
        this.icon = icon;
        this.menuType = menuType;
        this.parent = parent;
        this.displayOrder = displayOrder;
        this.visible = visible;
        this.requiredRole = requiredRole;
        this.requiredPermission = requiredPermission;
        this.systemItem = systemItem;
        this.createdBy = createdBy;
    }

    public void update(String label, String url, String icon, Role requiredRole, String requiredPermission) {
        this.label = label;
        this.url = url;
        this.icon = icon;
        this.requiredRole = requiredRole;
        this.requiredPermission = requiredPermission;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }

    public void setParent(MenuItem parent) {
        this.parent = parent;
    }
}
