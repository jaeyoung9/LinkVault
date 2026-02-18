package org.link.linkvault.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.link.linkvault.dto.AdFreePassResponseDto;
import org.link.linkvault.dto.RewardStatusResponseDto;
import org.link.linkvault.entity.*;
import org.link.linkvault.repository.AdFreePassRepository;
import org.link.linkvault.repository.RewardPointsLedgerRepository;
import org.link.linkvault.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpSession;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RewardService {

    private final RewardPointsLedgerRepository ledgerRepository;
    private final AdFreePassRepository adFreePassRepository;
    private final UserRepository userRepository;
    private final SystemSettingsService systemSettingsService;
    private final AuditLogService auditLogService;

    private static final String NONCE_SESSION_KEY = "reward_nonce";

    private int getIntSetting(String key, int defaultValue) {
        return systemSettingsService.getValue(key)
                .map(v -> { try { return Integer.parseInt(v); } catch (NumberFormatException e) { return defaultValue; } })
                .orElse(defaultValue);
    }

    public RewardStatusResponseDto getStatus(User user) {
        int points = user.getRewardPoints();
        int dailyCap = getIntSetting("reward.daily-video-cap", 5);
        int adFreeHoursCost = getIntSetting("reward.adfree-hours-cost", 50);
        int adFreeHoursDuration = getIntSetting("reward.adfree-hours-duration", 2);

        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        long todayVideos = ledgerRepository.countByUserIdAndActionAndCreatedAtAfter(
                user.getId(), RewardAction.VIDEO_WATCHED, startOfDay);
        int dailyRemaining = Math.max(0, dailyCap - (int) todayVideos);

        // Check for active reward pass
        List<AdFreePass> rewardPasses = adFreePassRepository.findActiveByUserId(user.getId(), LocalDateTime.now());
        AdFreePassResponseDto activePass = rewardPasses.stream()
                .filter(p -> p.getType() == AdFreePassType.REWARD_REDEMPTION)
                .findFirst()
                .map(AdFreePassResponseDto::from)
                .orElse(null);

        return RewardStatusResponseDto.builder()
                .points(points)
                .dailyVideosRemaining(dailyRemaining)
                .dailyVideosCap(dailyCap)
                .adFreeHoursCost(adFreeHoursCost)
                .adFreeHoursDuration(adFreeHoursDuration)
                .activeRewardPass(activePass)
                .build();
    }

    @Transactional
    public Map<String, Object> requestVideo(User user, HttpSession session) {
        int dailyCap = getIntSetting("reward.daily-video-cap", 5);
        int pointsPerVideo = getIntSetting("reward.video-points", 10);
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();

        long todayCount = ledgerRepository.countByUserIdAndActionAndCreatedAtAfter(
                user.getId(), RewardAction.VIDEO_WATCHED, startOfDay);

        if (todayCount >= dailyCap) {
            return Map.of("error", "Daily cap reached. Try again tomorrow.");
        }

        String nonce = UUID.randomUUID().toString();
        session.setAttribute(NONCE_SESSION_KEY, nonce);

        String adTagUrl = systemSettingsService.getValue("reward.ima-ad-tag-url").orElse("");

        return Map.of(
                "nonce", nonce,
                "adTagUrl", adTagUrl,
                "pointsPerVideo", pointsPerVideo
        );
    }

    @Transactional
    public Map<String, Object> completeVideo(User user, String nonce, HttpSession session) {
        String storedNonce = (String) session.getAttribute(NONCE_SESSION_KEY);
        if (storedNonce == null || !storedNonce.equals(nonce)) {
            throw new SecurityException("Invalid or expired video nonce");
        }

        // Remove nonce to prevent replay
        session.removeAttribute(NONCE_SESSION_KEY);

        int pointsPerVideo = getIntSetting("reward.video-points", 10);

        // Award points
        RewardPointsLedger entry = RewardPointsLedger.builder()
                .user(user)
                .points(pointsPerVideo)
                .action(RewardAction.VIDEO_WATCHED)
                .description("Watched reward video")
                .build();
        ledgerRepository.save(entry);

        user.addRewardPoints(pointsPerVideo);
        userRepository.save(user);

        return Map.of(
                "pointsEarned", pointsPerVideo,
                "newBalance", user.getRewardPoints()
        );
    }

    @Transactional
    public Map<String, Object> redeem(User user) {
        int cost = getIntSetting("reward.adfree-hours-cost", 50);
        int hours = getIntSetting("reward.adfree-hours-duration", 2);

        if (user.getRewardPoints() < cost) {
            throw new IllegalStateException("Insufficient points. Need " + cost + ", have " + user.getRewardPoints());
        }

        // Deduct points
        user.deductRewardPoints(cost);
        userRepository.save(user);

        // Create ledger entry
        RewardPointsLedger entry = RewardPointsLedger.builder()
                .user(user)
                .points(-cost)
                .action(RewardAction.REDEEM_AD_FREE)
                .description("Redeemed for " + hours + "h ad-free")
                .build();
        ledgerRepository.save(entry);

        // Create ad-free pass
        LocalDateTime now = LocalDateTime.now();
        AdFreePass pass = AdFreePass.builder()
                .user(user)
                .type(AdFreePassType.REWARD_REDEMPTION)
                .startsAt(now)
                .expiresAt(now.plusHours(hours))
                .build();
        adFreePassRepository.save(pass);

        auditLogService.log(user.getUsername(), AuditActionCodes.REWARD_REDEEM, "AdFreePass", pass.getId(),
                "cost=" + cost + " hours=" + hours);

        return Map.of(
                "pass", AdFreePassResponseDto.from(pass),
                "newBalance", user.getRewardPoints()
        );
    }
}
