package org.example.meetingscheduler.notification.service;

import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.retry.annotation.Retry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.example.meetingscheduler.notification.dto.NotificationRequestDto;
import org.springframework.stereotype.Service;

/**
 * Fake notification service that delegates to {@link NotificationFeignClient}.
 * Retries up to 3 attempts with exponential backoff (2 s, 4 s) on failure.
 * The underlying endpoint is expected to deduplicate using
 * {@code meetingId} + {@code participantId} as a natural idempotency key, so retries are safe.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FakeNotificationService {

    private final NotificationFeignClient notificationFeignClient;
    private final RetryRegistry retryRegistry;

    @PostConstruct
    public void registerRetryEventListeners() {
        this.retryRegistry.retry("notification").getEventPublisher()
            .onRetry(event -> log.warn(
                "Notification attempt {} failed, retrying: {}",
                event.getNumberOfRetryAttempts(),
                event.getLastThrowable() != null
                    ? event.getLastThrowable().getMessage()
                    : StringUtils.EMPTY
            ))
            .onError(event -> log.error(
                "All {} notification attempts exhausted: {}",
                event.getNumberOfRetryAttempts(),
                event.getLastThrowable() != null
                    ? event.getLastThrowable().getMessage()
                    : StringUtils.EMPTY
            ));
    }

    @Retry(name = "notification")
    public void notify(final NotificationRequestDto notificationRequestDto) {
        log.info("Sending {} notification: meetingId={}, participantId={}",
            notificationRequestDto.eventType(),
            notificationRequestDto.meetingId(),
            notificationRequestDto.participantId()
        );
        this.notificationFeignClient.notify(notificationRequestDto);
        log.info("Notification sent: meetingId={}, participantId={}",
            notificationRequestDto.meetingId(),
            notificationRequestDto.participantId()
        );
    }
}
