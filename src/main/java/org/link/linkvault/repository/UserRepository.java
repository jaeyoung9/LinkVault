package org.link.linkvault.repository;

import org.link.linkvault.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    @Query("SELECT COUNT(b) FROM Bookmark b WHERE b.user.id = :userId")
    long countBookmarksByUserId(@Param("userId") Long userId);

    @Query("SELECT u FROM User u WHERE u.enabled = true " +
           "AND u.role <> org.link.linkvault.entity.Role.SUPER_ADMIN " +
           "AND (u.privacyAgreedVersion IS NULL OR u.privacyAgreedVersion <> :activeVersion)")
    List<User> findEnabledUsersNotConsentedToVersion(@Param("activeVersion") int activeVersion);
}
