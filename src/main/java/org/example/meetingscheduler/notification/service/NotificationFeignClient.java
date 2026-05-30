package org.example.meetingscheduler.notification.service;

import org.example.meetingscheduler.notification.dto.NotificationRequestDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Feign client for the external notification endpoint.
 * The receiver is expected to deduplicate requests using the combination of
 * {@code meetingId} and {@code participantId} as a natural idempotency key,
 * ensuring that retries do not result in duplicate notifications.
 */
@FeignClient(name = "notification-service", url = "${notification.service.url}")
public interface NotificationFeignClient {

    @PostMapping("/notifications")
    void notify(@RequestBody final NotificationRequestDto notificationRequestDto);
}
