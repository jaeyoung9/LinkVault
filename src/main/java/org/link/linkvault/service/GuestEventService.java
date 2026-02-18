package org.link.linkvault.service;

import lombok.RequiredArgsConstructor;
import org.link.linkvault.dto.GuestFunnelStatsDto;
import org.link.linkvault.entity.GuestEvent;
import org.link.linkvault.entity.GuestEventType;
import org.link.linkvault.repository.GuestEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GuestEventService {

    private final GuestEventRepository guestEventRepository;

    @Transactional
    public void logEvent(String sessionId, GuestEventType eventType, String pageUrl) {
        // Rate limit: max 100 events per session per hour
        long recentCount = guestEventRepository.countBySessionIdAndCreatedAtAfter(
                sessionId, LocalDateTime.now().minusHours(1));
        if (recentCount >= 100) {
            return;
        }

        GuestEvent event = GuestEvent.builder()
                .sessionId(sessionId)
                .eventType(eventType)
                .pageUrl(pageUrl)
                .build();
        guestEventRepository.save(event);
    }

    public GuestFunnelStatsDto getFunnelStats(LocalDateTime from, LocalDateTime to) {
        return GuestFunnelStatsDto.builder()
                .pageViews(guestEventRepository.countDistinctSessionsByEventType(GuestEventType.PAGE_VIEW, from, to))
                .adShown(guestEventRepository.countDistinctSessionsByEventType(GuestEventType.AD_SHOWN, from, to))
                .adClicked(guestEventRepository.countDistinctSessionsByEventType(GuestEventType.AD_CLICKED, from, to))
                .adHidden(guestEventRepository.countDistinctSessionsByEventType(GuestEventType.AD_HIDDEN, from, to))
                .signupStarted(guestEventRepository.countDistinctSessionsByEventType(GuestEventType.SIGNUP_STARTED, from, to))
                .signupCompleted(guestEventRepository.countDistinctSessionsByEventType(GuestEventType.SIGNUP_COMPLETED, from, to))
                .build();
    }
}
