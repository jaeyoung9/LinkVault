package org.link.linkvault.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.link.linkvault.entity.PrivacyPolicy;
import org.link.linkvault.entity.User;
import org.link.linkvault.repository.PrivacyPolicyRepository;
import org.link.linkvault.repository.UserRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PrivacyPolicyScheduler {

    private final PrivacyPolicyRepository privacyPolicyRepository;
    private final UserRepository userRepository;

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void deactivateNonConsentedUsers() {
        PrivacyPolicy activePolicy = privacyPolicyRepository.findByActiveTrue().orElse(null);
        if (activePolicy == null) {
            return;
        }

        // Grace period: 30 days from policy creation
        LocalDateTime deadline = activePolicy.getCreatedAt().plusDays(30);
        if (LocalDateTime.now().isBefore(deadline)) {
            log.debug("Privacy policy v{} grace period active until {}", activePolicy.getVersion(), deadline);
            return;
        }

        List<User> nonConsentedUsers = userRepository.findEnabledUsersNotConsentedToVersion(activePolicy.getVersion());
        if (nonConsentedUsers.isEmpty()) {
            return;
        }

        for (User user : nonConsentedUsers) {
            user.deactivateForPrivacy("Auto-deactivated: did not consent to privacy policy v" + activePolicy.getVersion());
        }
        userRepository.saveAll(nonConsentedUsers);

        log.info("Auto-deactivated {} user(s) who did not consent to privacy policy v{}",
                nonConsentedUsers.size(), activePolicy.getVersion());
    }
}
