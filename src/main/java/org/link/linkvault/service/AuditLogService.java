package org.link.linkvault.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.link.linkvault.dto.AuditLogResponseDto;
import org.link.linkvault.entity.AuditLog;
import org.link.linkvault.entity.User;
import org.link.linkvault.repository.AuditLogRepository;
import org.link.linkvault.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(String username, String action, String entityType, Long entityId, String details) {
        try {
            User user = username != null ? userRepository.findByUsername(username).orElse(null) : null;
            AuditLog auditLog = new AuditLog(user, action, entityType, entityId, details);
            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.warn("AUDIT_PERSIST_FAILURE action={} entityType={} entityId={} actor={}: {}",
                    action, entityType, entityId, username, e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public Page<AuditLogResponseDto> findAll(Pageable pageable) {
        return auditLogRepository.findAllWithUser(pageable)
                .map(AuditLogResponseDto::from);
    }

    @Transactional(readOnly = true)
    public Page<AuditLogResponseDto> findByUserId(Long userId, Pageable pageable) {
        return auditLogRepository.findByUserId(userId, pageable)
                .map(AuditLogResponseDto::from);
    }

    @Transactional(readOnly = true)
    public Page<AuditLogResponseDto> findByAction(String action, Pageable pageable) {
        return auditLogRepository.findByAction(action, pageable)
                .map(AuditLogResponseDto::from);
    }
}
