package org.link.linkvault.dto;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;

@Getter
@Setter
public class TransparencyReportRequestDto {
    @NotBlank
    private String title;
    @NotBlank
    private String period;
    @NotBlank
    private String content;
    private int totalDonationsCents;
    private int totalPassRevenueCents;
}
