package org.link.linkvault.dto;

import lombok.Builder;
import lombok.Getter;
import org.link.linkvault.dto.AdFreePassResponseDto;

@Getter
@Builder
public class RewardStatusResponseDto {
    private int points;
    private int dailyVideosRemaining;
    private int dailyVideosCap;
    private int adFreeHoursCost;
    private int adFreeHoursDuration;
    private AdFreePassResponseDto activeRewardPass;
}
