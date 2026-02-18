package org.link.linkvault.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MonetizationStatsDto {
    private int totalRevenueCents;
    private long activePasses;
    private long activeSubscribers;
    private double guestConversionRate;
    private long totalDonations;
    private long totalDonationsCents;
}
