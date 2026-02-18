package org.link.linkvault.controller;

import lombok.RequiredArgsConstructor;
import org.link.linkvault.dto.GuestEventRequestDto;
import org.link.linkvault.entity.GuestEventType;
import org.link.linkvault.service.GuestEventService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/guest")
@RequiredArgsConstructor
public class GuestApiController {

    private final GuestEventService guestEventService;

    @PostMapping("/event")
    public ResponseEntity<Void> logEvent(@Valid @RequestBody GuestEventRequestDto request) {
        try {
            GuestEventType eventType = GuestEventType.valueOf(request.getEventType());
            guestEventService.logEvent(request.getSessionId(), eventType, request.getPageUrl());
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
