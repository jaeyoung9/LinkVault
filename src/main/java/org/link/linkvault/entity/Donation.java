package org.link.linkvault.entity;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "donations", indexes = {
        @Index(name = "idx_donation_user", columnList = "user_id"),
        @Index(name = "idx_donation_status", columnList = "status"),
        @Index(name = "idx_donation_created", columnList = "createdAt")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Donation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private int amountCents;

    @Column(nullable = false, length = 10)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DonationType donationType;

    @Column(length = 200)
    private String stripePaymentIntentId;

    @Column(length = 200)
    private String stripeSubscriptionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DonationStatus status;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime completedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @Builder
    public Donation(User user, int amountCents, String currency, DonationType donationType,
                    String stripePaymentIntentId, String stripeSubscriptionId, DonationStatus status) {
        this.user = user;
        this.amountCents = amountCents;
        this.currency = currency;
        this.donationType = donationType;
        this.stripePaymentIntentId = stripePaymentIntentId;
        this.stripeSubscriptionId = stripeSubscriptionId;
        this.status = status;
    }

    public void markCompleted() {
        this.status = DonationStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }

    public void markFailed() {
        this.status = DonationStatus.FAILED;
    }

    public void markRefunded() {
        this.status = DonationStatus.REFUNDED;
    }

    public void markCancelled() {
        this.status = DonationStatus.CANCELLED;
    }
}
