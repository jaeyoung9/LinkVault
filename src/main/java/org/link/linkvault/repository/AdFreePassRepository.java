package org.link.linkvault.repository;

import org.link.linkvault.entity.AdFreePass;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AdFreePassRepository extends JpaRepository<AdFreePass, Long> {

    @Query("SELECT p FROM AdFreePass p WHERE p.user.id = :userId AND p.active = true AND p.refunded = false AND p.expiresAt > :now")
    List<AdFreePass> findActiveByUserId(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    @Query("SELECT p FROM AdFreePass p WHERE p.user.id = :userId ORDER BY p.createdAt DESC")
    List<AdFreePass> findByUserId(@Param("userId") Long userId);

    @Query(value = "SELECT p FROM AdFreePass p LEFT JOIN FETCH p.user ORDER BY p.createdAt DESC",
           countQuery = "SELECT COUNT(p) FROM AdFreePass p")
    Page<AdFreePass> findAllWithUser(Pageable pageable);

    @Query("SELECT p FROM AdFreePass p WHERE p.stripePaymentIntentId = :paymentIntentId")
    List<AdFreePass> findByStripePaymentIntentId(@Param("paymentIntentId") String paymentIntentId);

    @Query("SELECT COUNT(p) FROM AdFreePass p WHERE p.active = true AND p.refunded = false AND p.expiresAt > :now")
    long countAllActive(@Param("now") LocalDateTime now);
}
