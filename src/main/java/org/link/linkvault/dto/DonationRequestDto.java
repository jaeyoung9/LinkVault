package org.link.linkvault.dto;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

@Getter
@Setter
public class DonationRequestDto {
    @Min(100)
    private int amountCents;
    private String currency = "usd";
    @NotNull
    private String donationType;
}
