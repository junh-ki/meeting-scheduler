package org.example.meetingscheduler.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.meetingscheduler.redis.RedisQueueKeys;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationEnqueueService {

    private final RedisTemplate<String, String> redisTemplate;

    public void enqueueSchedule(final Long meetingId) {
        enqueue(RedisQueueKeys.SCHEDULE_QUEUE, meetingId);
    }

    public void enqueueDelete(final Long meetingId) {
        enqueue(RedisQueueKeys.DELETE_QUEUE, meetingId);
    }

    private void enqueue(final String queueKey,
                         final Long meetingId) {
        try {
            final Boolean added = this.redisTemplate.opsForZSet()
                .addIfAbsent(queueKey, String.valueOf(meetingId), System.currentTimeMillis());
            if (Boolean.TRUE.equals(added)) {
                log.info("Enqueued meetingId={} to queue={}", meetingId, queueKey);
            } else {
                log.debug("meetingId={} already present in queue={}, skipping enqueue", meetingId, queueKey);
            }
        } catch (final RedisConnectionFailureException | RedisSystemException exception) {
            log.warn("Failed to enqueue meetingId={} to queue={} - recovery job will retry: {}",
                meetingId, queueKey, exception.getMessage());
        }
    }
}
