package org.link.linkvault.repository;

import org.link.linkvault.entity.AnnouncementPollOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnnouncementPollOptionRepository extends JpaRepository<AnnouncementPollOption, Long> {

    List<AnnouncementPollOption> findByAnnouncementIdOrderByDisplayOrderAsc(Long announcementId);

    void deleteByAnnouncementId(Long announcementId);
}
