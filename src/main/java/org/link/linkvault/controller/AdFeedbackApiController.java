package org.link.linkvault.controller;

import lombok.RequiredArgsConstructor;
import org.link.linkvault.dto.AdHideFeedbackRequestDto;
import org.link.linkvault.entity.AdHideFeedback;
import org.link.linkvault.entity.AdHideReason;
import org.link.linkvault.entity.User;
import org.link.linkvault.repository.AdHideFeedbackRepository;
import org.link.linkvault.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/ad")
@RequiredArgsConstructor
public class AdFeedbackApiController {

    private final AdHideFeedbackRepository adHideFeedbackRepository;
    private final UserService userService;

    @PostMapping("/hide")
    public ResponseEntity<Void> hideAd(@Valid @RequestBody AdHideFeedbackRequestDto request,
                                        @AuthenticationPrincipal UserDetails userDetails) {
        try {
            AdHideReason reason = AdHideReason.valueOf(request.getReason());
            User user = null;
            if (userDetails != null) {
                user = userService.getUserEntity(userDetails.getUsername());
            }

            AdHideFeedback feedback = AdHideFeedback.builder()
                    .user(user)
                    .sessionId(request.getSessionId())
                    .adUnitId(request.getAdUnitId())
                    .reason(reason)
                    .build();
            adHideFeedbackRepository.save(feedback);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
