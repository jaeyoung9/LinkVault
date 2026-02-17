package org.link.linkvault.repository;

import org.link.linkvault.entity.PrivacyPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PrivacyPolicyRepository extends JpaRepository<PrivacyPolicy, Long> {

    Optional<PrivacyPolicy> findByActiveTrue();

    @Query("SELECT p FROM PrivacyPolicy p LEFT JOIN FETCH p.updatedBy ORDER BY p.version DESC")
    List<PrivacyPolicy> findAllByOrderByVersionDesc();
}
