package org.link.linkvault.repository;

import org.link.linkvault.entity.AnnouncementVote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AnnouncementVoteRepository extends JpaRepository<AnnouncementVote, Long> {

    Optional<AnnouncementVote> findByUserIdAndAnnouncementId(Long userId, Long announcementId);

    void deleteByAnnouncementId(Long announcementId);
}
