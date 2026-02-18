package org.link.linkvault.service;

import lombok.RequiredArgsConstructor;
import org.link.linkvault.dto.DonationResponseDto;
import org.link.linkvault.entity.DonationType;
import org.link.linkvault.entity.User;
import org.link.linkvault.repository.DonationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DonationService {

    private final DonationRepository donationRepository;
    private final StripeService stripeService;

    public String createCheckout(User user, int amountCents, DonationType donationType,
                                  String successUrl, String cancelUrl) {
        return stripeService.createDonationCheckoutSession(user, amountCents, donationType, successUrl, cancelUrl);
    }

    public Page<DonationResponseDto> getHistory(User user, Pageable pageable) {
        return donationRepository.findByUserId(user.getId(), pageable)
                .map(DonationResponseDto::from);
    }
}
