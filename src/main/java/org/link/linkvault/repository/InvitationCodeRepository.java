package org.link.linkvault.repository;

import org.link.linkvault.entity.InvitationCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import javax.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface InvitationCodeRepository extends JpaRepository<InvitationCode, Long> {

    Optional<InvitationCode> findByCode(String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ic FROM InvitationCode ic WHERE ic.id = :id")
    Optional<InvitationCode> findByIdForUpdate(Long id);

    @Query("SELECT ic FROM InvitationCode ic LEFT JOIN FETCH ic.createdBy ORDER BY ic.createdAt DESC")
    List<InvitationCode> findAllWithCreator();

    List<InvitationCode> findByCreatedById(Long userId);
}
