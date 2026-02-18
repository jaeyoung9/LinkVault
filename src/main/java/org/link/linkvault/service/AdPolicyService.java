package org.link.linkvault.service;

import lombok.RequiredArgsConstructor;
import org.link.linkvault.entity.User;
import org.link.linkvault.repository.AdFreePassRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdPolicyService {

    private final SystemSettingsService systemSettingsService;
    private final AdFreePassRepository adFreePassRepository;

    public boolean isAdsEnabled() {
        return systemSettingsService.getValue("feature.ads-enabled")
                .map("true"::equals).orElse(false);
    }

    public boolean isAdFree(User user) {
        if (user == null) return false;
        return !adFreePassRepository.findActiveByUserId(user.getId(), LocalDateTime.now()).isEmpty();
    }

    public boolean shouldShowAds(User user) {
        if (!isAdsEnabled()) return false;
        if (user != null && isAdFree(user)) return false;
        return true;
    }

    public List<Integer> getAdInsertionPositions(int totalPosts, boolean isGuest, int sessionPageViewCount) {
        if (!isAdsEnabled() || totalPosts == 0) return Collections.emptyList();

        int interval = getIntSetting("ad.feed-insertion-interval", 6);
        int maxPerPage = getIntSetting("ad.max-per-page", 3);
        int gracePageViews = getIntSetting("ad.guest-first-session-grace", 3);
        double guestMultiplier = getDoubleSetting("ad.guest-frequency-multiplier", 1.5);

        if (isGuest) {
            if (sessionPageViewCount < gracePageViews) return Collections.emptyList();
            interval = (int) Math.ceil(interval / guestMultiplier);
        }

        if (interval < 1) interval = 1;

        List<Integer> positions = new ArrayList<>();
        for (int i = interval - 1; i < totalPosts; i += interval) {
            if (positions.size() >= maxPerPage) break;
            positions.add(i);
        }
        return positions;
    }

    public String getAdsenseClientId() {
        return systemSettingsService.getValue("ad.adsense-client-id").orElse("");
    }

    public String getAdsenseSlotFeed() {
        return systemSettingsService.getValue("ad.adsense-slot-feed").orElse("");
    }

    public String getAdsenseLayoutKey() {
        return systemSettingsService.getValue("ad.adsense-layout-key").orElse("");
    }

    private int getIntSetting(String key, int defaultValue) {
        return systemSettingsService.getValue(key)
                .map(v -> {
                    try { return Integer.parseInt(v); }
                    catch (NumberFormatException e) { return defaultValue; }
                }).orElse(defaultValue);
    }

    private double getDoubleSetting(String key, double defaultValue) {
        return systemSettingsService.getValue(key)
                .map(v -> {
                    try { return Double.parseDouble(v); }
                    catch (NumberFormatException e) { return defaultValue; }
                }).orElse(defaultValue);
    }
}
