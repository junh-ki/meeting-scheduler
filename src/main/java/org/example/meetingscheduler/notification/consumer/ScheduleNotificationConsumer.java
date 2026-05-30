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
import org.example.meetingscheduler.meeting.MeetingEntity;
import org.example.meetingscheduler.notification.dto.NotificationEventType;
import org.example.meetingscheduler.notification.dto.NotificationRequestDto;
import org.example.meetingscheduler.notification.dto.NotificationStatus;
import org.example.meetingscheduler.notification.entity.ScheduleNotificationEntity;
import org.example.meetingscheduler.notification.repository.ScheduleNotificationRepository;
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
public class ScheduleNotificationConsumer {

    private final RedisTemplate<String, String> redisTemplate;
    private final ScheduleNotificationRepository scheduleNotificationRepository;
    private final FakeNotificationService fakeNotificationService;
    private final PlatformTransactionManager transactionManager;
    private final ExecutorService executorService = Executors.newFixedThreadPool(5);

    @Scheduled(fixedDelay = 5000)
    public void consume() {
        final Set<ZSetOperations.TypedTuple<String>> entries =
            this.redisTemplate.opsForZSet().popMin(RedisQueueKeys.SCHEDULE_QUEUE, Long.MAX_VALUE);
        if (CollectionUtils.isEmpty(entries)) {
            return;
        }
        log.info("Dequeued {} meeting(s) from schedule queue", entries.size());
        entries.stream()
            .map(ZSetOperations.TypedTuple::getValue)
            .filter(Objects::nonNull)
            .map(Long::parseLong)
            .flatMap(meetingId -> this.scheduleNotificationRepository
                .findAllByMeetingIdAndStatus(meetingId, NotificationStatus.PENDING)
                .stream())
            .forEach(notification ->
                this.executorService.submit(() -> process(notification.getId())));
    }

    private void process(final Long notificationId) {
        new TransactionTemplate(this.transactionManager).execute(status -> {
            final ScheduleNotificationEntity notification = this.scheduleNotificationRepository
                .findWithLockById(notificationId)
                .orElseThrow(() -> new IllegalStateException("ScheduleNotification not found: " + notificationId));
            if (NotificationStatus.PENDING != notification.getStatus()) {
                log.debug("Schedule notification id={} already processed (status={}), skipping", notificationId, notification.getStatus());
                return null;
            }
            final MeetingEntity meetingEntity = notification.getMeeting();
            final UserEntity participantUserEntity = notification.getParticipant();
            log.info("Processing schedule notification: notificationId={}, meetingId={}, participantId={}",
                notificationId, meetingEntity.getId(), participantUserEntity.getId());
            try {
                this.fakeNotificationService.notify(
                    new NotificationRequestDto(
                        meetingEntity.getId(),
                        meetingEntity.getTitle(),
                        meetingEntity.getStartTime(),
                        meetingEntity.getEndTime(),
                        NotificationEventType.MEETING_CREATED,
                        participantUserEntity.getId(),
                        participantUserEntity.getName(),
                        participantUserEntity.getEmail()
                    )
                );
                notification.setStatus(NotificationStatus.COMPLETED);
                notification.setLastAttemptAt(OffsetDateTime.now());
                log.info("Schedule notification completed: notificationId={}, meetingId={}, participantId={}",
                    notificationId, meetingEntity.getId(), participantUserEntity.getId());
            } catch (final FeignException feignException) {
                log.warn("Schedule notification failed for id={}: {}", notificationId, feignException.getMessage());
                notification.setStatus(NotificationStatus.FAILED);
                notification.setErrorMessage(feignException.getMessage());
                notification.setLastAttemptAt(OffsetDateTime.now());
            }
            return null;
        });
    }
}
