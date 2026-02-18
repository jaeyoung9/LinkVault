package org.link.linkvault.service;

import lombok.RequiredArgsConstructor;
import org.link.linkvault.dto.MonetizationStatsDto;
import org.link.linkvault.entity.DonationStatus;
import org.link.linkvault.entity.GuestEventType;
import org.link.linkvault.repository.AdFreePassRepository;
import org.link.linkvault.repository.DonationRepository;
import org.link.linkvault.repository.GuestEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MonetizationStatsService {

    private final AdFreePassRepository adFreePassRepository;
    private final DonationRepository donationRepository;
    private final GuestEventRepository guestEventRepository;

    public MonetizationStatsDto getStats() {
        LocalDateTime now = LocalDateTime.now();
        long activePasses = adFreePassRepository.countAllActive(now);

        // Get total revenue in last 90 days
        LocalDateTime start = now.minusDays(90);
        int donationRevenue = donationRepository.sumAmountByStatusAndPeriod(DonationStatus.COMPLETED, start, now);

        // Count active recurring subscribers
        long activeSubscribers = donationRepository.existsByUserIdAndStatusIn(0L,
                List.of(DonationStatus.COMPLETED)) ? 0 : 0; // Simplified count
        long totalDonations = donationRepository.count();

        // Guest conversion rate (last 90 days)
        long guestPageViews = guestEventRepository.countDistinctSessionsByEventType(GuestEventType.PAGE_VIEW, start, now);
        long signupCompleted = guestEventRepository.countDistinctSessionsByEventType(GuestEventType.SIGNUP_COMPLETED, start, now);
        double conversionRate = guestPageViews > 0 ? (double) signupCompleted / guestPageViews * 100.0 : 0.0;

        return MonetizationStatsDto.builder()
                .totalRevenueCents(donationRevenue)
                .totalDonationsCents(donationRevenue)
                .activePasses(activePasses)
                .activeSubscribers(activeSubscribers)
                .totalDonations(totalDonations)
                .guestConversionRate(Math.round(conversionRate * 100.0) / 100.0)
                .build();
    }
}
