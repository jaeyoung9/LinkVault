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
@Table(name = "invitation_codes", indexes = {
        @Index(name = "idx_invitation_code", columnList = "code")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InvitationCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(nullable = false)
    private int maxUses = 1;

    @Column(nullable = false)
    private int currentUses = 0;

    @Column(nullable = false)
    private boolean active = true;

    private LocalDateTime expiresAt;

    @Column(length = 200)
    private String note;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Role assignedRole;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "invitationCode", cascade = CascadeType.ALL)
    private List<InvitationUse> uses = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @Builder
    public InvitationCode(String code, User createdBy, int maxUses, boolean active,
                          LocalDateTime expiresAt, String note, Role assignedRole) {
        this.code = code;
        this.createdBy = createdBy;
        this.maxUses = maxUses;
        this.active = active;
        this.expiresAt = expiresAt;
        this.note = note;
        this.assignedRole = assignedRole;
    }

    public boolean isUsable() {
        if (!active) return false;
        if (expiresAt != null && LocalDateTime.now().isAfter(expiresAt)) return false;
        if (maxUses > 0 && currentUses >= maxUses) return false;
        return true;
    }

    public void recordUse() {
        this.currentUses++;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
