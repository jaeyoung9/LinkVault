package org.link.linkvault.repository;

import org.link.linkvault.entity.Donation;
import org.link.linkvault.entity.DonationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface DonationRepository extends JpaRepository<Donation, Long> {

    @Query("SELECT d FROM Donation d WHERE d.user.id = :userId ORDER BY d.createdAt DESC")
    Page<Donation> findByUserId(@Param("userId") Long userId, Pageable pageable);

    @Query(value = "SELECT d FROM Donation d LEFT JOIN FETCH d.user ORDER BY d.createdAt DESC",
           countQuery = "SELECT COUNT(d) FROM Donation d")
    Page<Donation> findAllWithUser(Pageable pageable);

    Optional<Donation> findByStripePaymentIntentId(String stripePaymentIntentId);

    Optional<Donation> findByStripeSubscriptionId(String stripeSubscriptionId);

    @Query("SELECT COALESCE(SUM(d.amountCents), 0) FROM Donation d WHERE d.status = :status AND d.createdAt BETWEEN :from AND :to")
    int sumAmountByStatusAndPeriod(@Param("status") DonationStatus status,
                                   @Param("from") LocalDateTime from,
                                   @Param("to") LocalDateTime to);

    boolean existsByUserIdAndStatusIn(Long userId, List<DonationStatus> statuses);
}
