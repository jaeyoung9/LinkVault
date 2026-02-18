package org.link.linkvault.entity;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "stripe_customers", indexes = {
        @Index(name = "idx_stripe_user", columnList = "user_id"),
        @Index(name = "idx_stripe_customer_id", columnList = "stripeCustomerId")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StripeCustomer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false, unique = true, length = 100)
    private String stripeCustomerId;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @Builder
    public StripeCustomer(User user, String stripeCustomerId) {
        this.user = user;
        this.stripeCustomerId = stripeCustomerId;
    }
}
