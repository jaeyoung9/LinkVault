package org.link.linkvault.repository;

import org.link.linkvault.entity.AnnouncementRead;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AnnouncementReadRepository extends JpaRepository<AnnouncementRead, Long> {

    Optional<AnnouncementRead> findByUserIdAndAnnouncementId(Long userId, Long announcementId);

    boolean existsByUserIdAndAnnouncementId(Long userId, Long announcementId);
}
