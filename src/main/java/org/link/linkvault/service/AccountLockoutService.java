package org.link.linkvault.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.link.linkvault.entity.User;
import org.link.linkvault.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountLockoutService {

    private final UserRepository userRepository;
    private final SystemSettingsService systemSettingsService;
    private final AuditLogService auditLogService;

    private int getMaxAttempts() {
        return systemSettingsService.getValue("security.lockout-threshold")
                .map(Integer::parseInt).orElse(5);
    }

    private int getLockDurationMinutes() {
        return systemSettingsService.getValue("security.lockout-duration-minutes")
                .map(Integer::parseInt).orElse(30);
    }

    @Transactional
    public void recordFailedLogin(String username) {
        userRepository.findByUsername(username).ifPresent(user -> {
            // If lock has expired, reset before counting
            if (user.getAccountLockedUntil() != null && !user.isAccountLocked()) {
                user.resetFailedLoginAttempts();
            }

            user.recordFailedLogin();

            int maxAttempts = getMaxAttempts();
            if (user.getFailedLoginAttempts() >= maxAttempts) {
                int lockMinutes = getLockDurationMinutes();
                user.lockAccount(LocalDateTime.now().plusMinutes(lockMinutes));
                log.warn("Account locked for user '{}' after {} failed attempts. Locked for {} minutes.",
                        username, user.getFailedLoginAttempts(), lockMinutes);
                auditLogService.log(null, AuditActionCodes.ACCOUNT_LOCKED, "User", user.getId(),
                        "Account locked after " + user.getFailedLoginAttempts() + " failed login attempts. Lock duration: " + lockMinutes + " minutes.");
            }

            userRepository.save(user);
        });
    }

    @Transactional
    public void resetOnSuccess(String username) {
        userRepository.findByUsername(username).ifPresent(user -> {
            if (user.getFailedLoginAttempts() > 0 || user.getAccountLockedUntil() != null) {
                user.resetFailedLoginAttempts();
                userRepository.save(user);
            }
        });
    }

    @Transactional(readOnly = true)
    public boolean isLocked(String username) {
        return userRepository.findByUsername(username)
                .map(User::isAccountLocked)
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public List<User> getLockedUsers() {
        return userRepository.findLockedUsers();
    }

    @Transactional
    public void unlockUser(Long userId, String actorUsername) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));
        user.resetFailedLoginAttempts();
        userRepository.save(user);
        auditLogService.log(actorUsername, AuditActionCodes.ACCOUNT_UNLOCK, "User", userId,
                "Account manually unlocked by admin");
        log.info("Account unlocked for user '{}' by admin '{}'", user.getUsername(), actorUsername);
    }
}
