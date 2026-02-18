package org.link.linkvault.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GuestFunnelStatsDto {
    private long pageViews;
    private long adShown;
    private long adClicked;
    private long adHidden;
    private long signupStarted;
    private long signupCompleted;
}
