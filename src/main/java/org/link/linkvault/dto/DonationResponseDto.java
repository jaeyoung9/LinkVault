package org.link.linkvault.dto;

import lombok.Builder;
import lombok.Getter;
import org.link.linkvault.entity.Donation;

import java.time.LocalDateTime;

@Getter
@Builder
public class DonationResponseDto {
    private Long id;
    private int amountCents;
    private String currency;
    private String donationType;
    private String status;
    private LocalDateTime createdAt;

    public static DonationResponseDto from(Donation d) {
        return DonationResponseDto.builder()
                .id(d.getId())
                .amountCents(d.getAmountCents())
                .currency(d.getCurrency())
                .donationType(d.getDonationType().name())
                .status(d.getStatus().name())
                .createdAt(d.getCreatedAt())
                .build();
    }
}
