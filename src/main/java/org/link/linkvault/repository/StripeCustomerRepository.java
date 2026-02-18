package org.link.linkvault.repository;

import org.link.linkvault.entity.StripeCustomer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StripeCustomerRepository extends JpaRepository<StripeCustomer, Long> {

    Optional<StripeCustomer> findByUserId(Long userId);

    Optional<StripeCustomer> findByStripeCustomerId(String stripeCustomerId);
}
