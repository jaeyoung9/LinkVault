package org.link.linkvault.entity;

import javax.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "invitation_uses")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InvitationUse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invitation_code_id", nullable = false)
    private InvitationCode invitationCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, updatable = false)
    private LocalDateTime usedAt;

    public InvitationUse(InvitationCode invitationCode, User user) {
        this.invitationCode = invitationCode;
        this.user = user;
        this.usedAt = LocalDateTime.now();
    }
}
