package org.example.meetingscheduler.notification.service;

import org.example.meetingscheduler.redis.RedisQueueKeys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationEnqueueServiceTest {

    @InjectMocks
    private NotificationEnqueueService notificationEnqueueService;
    @Mock
    private RedisTemplate<String, String> redisTemplate;
    @Mock
    private ZSetOperations<String, String> zSetOperations;

    @BeforeEach
    void setUp() {
        when(this.redisTemplate.opsForZSet()).thenReturn(this.zSetOperations);
    }

    @Nested
    class enqueueSchedule {

        @Test
        void addsToScheduleQueueWithMeetingIdAsKey() {
            // Arrange
            when(zSetOperations.addIfAbsent(eq(RedisQueueKeys.SCHEDULE_QUEUE), eq("42"), anyDouble()))
                .thenReturn(true);

            // Act
            notificationEnqueueService.enqueueSchedule(42L);

            // Assert
            verify(zSetOperations).addIfAbsent(eq(RedisQueueKeys.SCHEDULE_QUEUE), eq("42"), anyDouble());
        }

        @Test
        void doesNotThrowWhenMeetingIdAlreadyPresent() {
            // Arrange
            when(zSetOperations.addIfAbsent(eq(RedisQueueKeys.SCHEDULE_QUEUE), eq("42"), anyDouble()))
                .thenReturn(false);

            // Act & Assert
            assertThatNoException().isThrownBy(() -> notificationEnqueueService.enqueueSchedule(42L));
        }

        @Test
        void doesNotPropagateOnConnectionFailure() {
            // Arrange
            doThrow(new RedisConnectionFailureException("down"))
                .when(zSetOperations).addIfAbsent(eq(RedisQueueKeys.SCHEDULE_QUEUE), eq("42"), anyDouble());

            // Act & Assert
            assertThatNoException().isThrownBy(() -> notificationEnqueueService.enqueueSchedule(42L));
        }

        @Test
        void doesNotPropagateOnRedisSystemException() {
            // Arrange
            doThrow(new RedisSystemException("bad command", null))
                .when(zSetOperations).addIfAbsent(eq(RedisQueueKeys.SCHEDULE_QUEUE), eq("42"), anyDouble());

            // Act & Assert
            assertThatNoException().isThrownBy(() -> notificationEnqueueService.enqueueSchedule(42L));
        }
    }

    @Test
    void addsToDeleteQueueWithMeetingIdAsKey() {
        // Arrange
        when(this.zSetOperations.addIfAbsent(eq(RedisQueueKeys.DELETE_QUEUE), eq("7"), anyDouble()))
            .thenReturn(true);

        // Act
        this.notificationEnqueueService.enqueueDelete(7L);

        // Assert
        verify(this.zSetOperations).addIfAbsent(eq(RedisQueueKeys.DELETE_QUEUE), eq("7"), anyDouble());
    }
}
