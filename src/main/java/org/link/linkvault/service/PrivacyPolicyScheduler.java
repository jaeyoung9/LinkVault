package org.link.linkvault.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.link.linkvault.entity.PrivacyPolicy;
import org.link.linkvault.repository.PrivacyPolicyRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class PrivacyPolicyScheduler {

    private final PrivacyPolicyRepository privacyPolicyRepository;
    private final UserService userService;

    @Scheduled(cron = "0 0 3 * * *")
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

        int count = userService.bulkDeactivateNonConsented(
                "Auto-deactivated: did not consent to privacy policy");
        if (count > 0) {
            log.info("Auto-deactivated {} user(s) who did not consent to privacy policy v{}",
                    count, activePolicy.getVersion());
        }
    }
}
