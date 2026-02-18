package org.link.linkvault.dto;

import lombok.Builder;
import lombok.Getter;
import org.link.linkvault.entity.AdFreePass;

import java.time.LocalDateTime;

@Getter
@Builder
public class AdFreePassResponseDto {
    private Long id;
    private String type;
    private LocalDateTime startsAt;
    private LocalDateTime expiresAt;
    private boolean active;
    private boolean refunded;
    private String username;
    private String stripePaymentIntentId;

    public static AdFreePassResponseDto from(AdFreePass pass) {
        return AdFreePassResponseDto.builder()
                .id(pass.getId())
                .type(pass.getType().name())
                .startsAt(pass.getStartsAt())
                .expiresAt(pass.getExpiresAt())
                .active(pass.isCurrentlyActive())
                .refunded(pass.isRefunded())
                .username(pass.getUser() != null ? pass.getUser().getUsername() : null)
                .stripePaymentIntentId(pass.getStripePaymentIntentId())
                .build();
    }
}
