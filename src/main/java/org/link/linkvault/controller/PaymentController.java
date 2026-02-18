package org.link.linkvault.controller;

import lombok.RequiredArgsConstructor;
import org.link.linkvault.dto.AdFreePassResponseDto;
import org.link.linkvault.dto.CheckoutRequestDto;
import org.link.linkvault.entity.AdFreePass;
import org.link.linkvault.entity.AdFreePassType;
import org.link.linkvault.entity.User;
import org.link.linkvault.repository.AdFreePassRepository;
import org.link.linkvault.service.StripeService;
import org.link.linkvault.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final StripeService stripeService;
    private final UserService userService;
    private final AdFreePassRepository adFreePassRepository;

    @PostMapping("/checkout")
    public ResponseEntity<?> createCheckout(@Valid @RequestBody CheckoutRequestDto request,
                                             @AuthenticationPrincipal UserDetails userDetails,
                                             HttpServletRequest httpRequest) {
        User user = userService.getUserEntity(userDetails.getUsername());
        AdFreePassType passType = AdFreePassType.valueOf(request.getPassType());

        String baseUrl = httpRequest.getScheme() + "://" + httpRequest.getServerName() + ":" + httpRequest.getServerPort();
        String successUrl = baseUrl + "/payment/success";
        String cancelUrl = baseUrl + "/payment/cancel";

        String checkoutUrl = stripeService.createAdFreeCheckoutSession(user, passType, successUrl, cancelUrl);
        return ResponseEntity.ok(Map.of("url", checkoutUrl));
    }

    @GetMapping("/pass-status")
    public ResponseEntity<?> getPassStatus(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.getUserEntity(userDetails.getUsername());
        List<AdFreePass> activePasses = adFreePassRepository.findActiveByUserId(user.getId(), LocalDateTime.now());
        List<AdFreePassResponseDto> dtos = activePasses.stream()
                .map(AdFreePassResponseDto::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(Map.of("passes", dtos, "isAdFree", !dtos.isEmpty()));
    }
}
