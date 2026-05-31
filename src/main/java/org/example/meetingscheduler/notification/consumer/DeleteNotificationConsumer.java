package org.example.meetingscheduler.notification.consumer;

import feign.FeignException;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.example.meetingscheduler.notification.dto.NotificationEventType;
import org.example.meetingscheduler.notification.dto.NotificationRequestDto;
import org.example.meetingscheduler.notification.dto.NotificationStatus;
import org.example.meetingscheduler.notification.entity.DeleteNotificationEntity;
import org.example.meetingscheduler.notification.repository.DeleteNotificationRepository;
import org.example.meetingscheduler.notification.service.FakeNotificationService;
import org.example.meetingscheduler.redis.RedisQueueKeys;
import org.example.meetingscheduler.user.UserEntity;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeleteNotificationConsumer {

    private final RedisTemplate<String, String> redisTemplate;
    private final DeleteNotificationRepository deleteNotificationRepository;
    private final FakeNotificationService fakeNotificationService;
    private final PlatformTransactionManager transactionManager;
    private final ExecutorService executorService = Executors.newFixedThreadPool(5);

    @Scheduled(fixedDelay = 5000)
    public void consume() {
        final Set<ZSetOperations.TypedTuple<String>> entries =
            this.redisTemplate.opsForZSet().popMin(RedisQueueKeys.DELETE_QUEUE, Long.MAX_VALUE);
        if (CollectionUtils.isEmpty(entries)) {
            return;
        }
        log.info("Dequeued {} meeting(s) from delete queue", entries.size());
        entries.stream()
            .map(ZSetOperations.TypedTuple::getValue)
            .filter(Objects::nonNull)
            .map(Long::parseLong)
            .flatMap(meetingId -> this.deleteNotificationRepository
                .findAllByMeetingIdAndStatus(meetingId, NotificationStatus.PENDING)
                .stream())
            .forEach(notification ->
                this.executorService.submit(() -> process(notification.getId())));
    }

    private void process(final Long notificationId) {
        new TransactionTemplate(this.transactionManager).execute(status -> {
            final DeleteNotificationEntity notification = this.deleteNotificationRepository
                .findWithLockById(notificationId)
                .orElseThrow(() -> new IllegalStateException("DeleteNotification not found: " + notificationId));
            if (notification.getStatus() != NotificationStatus.PENDING) {
                log.debug("Delete notification id={} already processed (status={}), skipping", notificationId, notification.getStatus());
                return null;
            }
            final UserEntity participantUserEntity = notification.getParticipant();
            log.info("Processing delete notification: notificationId={}, meetingId={}, participantId={}",
                notificationId, notification.getMeetingId(), participantUserEntity.getId());
            try {
                this.fakeNotificationService.notify(
                    new NotificationRequestDto(
                        notification.getMeetingId(),
                        notification.getMeetingTitle(),
                        notification.getMeetingStartTime(),
                        notification.getMeetingEndTime(),
                        NotificationEventType.MEETING_DELETED,
                        participantUserEntity.getId(),
                        participantUserEntity.getName(),
                        participantUserEntity.getEmail()
                    )
                );
                notification.setStatus(NotificationStatus.COMPLETED);
                notification.setLastAttemptAt(OffsetDateTime.now());
                log.info("Delete notification completed: notificationId={}, meetingId={}, participantId={}",
                    notificationId, notification.getMeetingId(), participantUserEntity.getId());
            } catch (final FeignException feignException) {
                log.warn("Delete notification failed for id={}: {}", notificationId, feignException.getMessage());
                notification.setStatus(NotificationStatus.FAILED);
                notification.setErrorMessage(feignException.getMessage());
                notification.setLastAttemptAt(OffsetDateTime.now());
            }
            return null;
        });
    }
}
