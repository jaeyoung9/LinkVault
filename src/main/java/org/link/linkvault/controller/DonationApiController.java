package org.link.linkvault.controller;

import lombok.RequiredArgsConstructor;
import org.link.linkvault.dto.DonationRequestDto;
import org.link.linkvault.dto.DonationResponseDto;
import org.link.linkvault.entity.DonationType;
import org.link.linkvault.entity.User;
import org.link.linkvault.service.DonationService;
import org.link.linkvault.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.Map;

@RestController
@RequestMapping("/api/donation")
@RequiredArgsConstructor
public class DonationApiController {

    private final DonationService donationService;
    private final UserService userService;

    @PostMapping("/checkout")
    public ResponseEntity<?> createCheckout(@Valid @RequestBody DonationRequestDto request,
                                             @AuthenticationPrincipal UserDetails userDetails,
                                             HttpServletRequest httpRequest) {
        User user = userService.getUserEntity(userDetails.getUsername());
        DonationType donationType = DonationType.valueOf(request.getDonationType());

        String baseUrl = httpRequest.getScheme() + "://" + httpRequest.getServerName() + ":" + httpRequest.getServerPort();
        String successUrl = baseUrl + "/donation/thankyou";
        String cancelUrl = baseUrl + "/settings";

        String checkoutUrl = donationService.createCheckout(user, request.getAmountCents(), donationType, successUrl, cancelUrl);
        return ResponseEntity.ok(Map.of("url", checkoutUrl));
    }

    @GetMapping("/history")
    public ResponseEntity<Page<DonationResponseDto>> getHistory(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        User user = userService.getUserEntity(userDetails.getUsername());
        return ResponseEntity.ok(donationService.getHistory(user,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))));
    }
}
