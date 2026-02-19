package org.link.linkvault.repository;

import org.link.linkvault.entity.AnnouncementPollVote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AnnouncementPollVoteRepository extends JpaRepository<AnnouncementPollVote, Long> {

    Optional<AnnouncementPollVote> findByUserIdAndAnnouncementId(Long userId, Long announcementId);

    boolean existsByAnnouncementId(Long announcementId);

    void deleteByAnnouncementId(Long announcementId);
}
