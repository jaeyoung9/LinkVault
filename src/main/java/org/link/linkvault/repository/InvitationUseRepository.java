package org.link.linkvault.repository;

import org.link.linkvault.entity.InvitationUse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InvitationUseRepository extends JpaRepository<InvitationUse, Long> {

    List<InvitationUse> findByInvitationCodeId(Long invitationCodeId);
}
