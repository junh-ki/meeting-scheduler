package org.example.meetingscheduler.notification.consumer;

import feign.FeignException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.awaitility.Awaitility;
import org.example.meetingscheduler.meeting.MeetingEntity;
import org.example.meetingscheduler.notification.dto.NotificationStatus;
import org.example.meetingscheduler.notification.entity.ScheduleNotificationEntity;
import org.example.meetingscheduler.notification.repository.ScheduleNotificationRepository;
import org.example.meetingscheduler.notification.service.FakeNotificationService;
import org.example.meetingscheduler.redis.RedisQueueKeys;
import org.example.meetingscheduler.user.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScheduleNotificationConsumerTest {

    private static final LocalDateTime START = LocalDateTime.of(2026, 5, 10, 10, 0);
    private static final LocalDateTime END = LocalDateTime.of(2026, 5, 10, 11, 0);
    @InjectMocks
    private ScheduleNotificationConsumer scheduleNotificationConsumer;
    @Mock
    private RedisTemplate<String, String> redisTemplate;
    @Mock
    private ZSetOperations<String, String> zSetOperations;
    @Mock
    private ScheduleNotificationRepository scheduleNotificationRepository;
    @Mock
    private FakeNotificationService fakeNotificationService;
    @Mock
    private PlatformTransactionManager platformTransactionManager;

    @BeforeEach
    void setUp() {
        when(this.redisTemplate.opsForZSet()).thenReturn(this.zSetOperations);
        lenient().when(this.platformTransactionManager.getTransaction(any())).thenReturn(mock(TransactionStatus.class));
    }

    private MeetingEntity meeting() {
        return MeetingEntity.builder()
            .id(42L).title("Sync").startTime(START).endTime(END)
            .organizer(UserEntity.builder().id(1L).name("Alice").email("alice@example.com").build())
            .participants(List.of()).build();
    }

    @SuppressWarnings("unchecked")
    private ZSetOperations.TypedTuple<String> entryOf(final String value) {
        final ZSetOperations.TypedTuple<String> entry = mock(ZSetOperations.TypedTuple.class);
        when(entry.getValue()).thenReturn(value);
        return entry;
    }

    private ScheduleNotificationEntity pendingNotification(final long id) {
        return ScheduleNotificationEntity.builder()
            .id(id).meeting(meeting())
            .participant(UserEntity.builder().id(2L).name("Bob").email("bob@example.com").build())
            .status(NotificationStatus.PENDING).build();
    }

    @Test
    void consume_doesNothingWhenQueueIsEmpty() {
        // Arrange
        when(this.zSetOperations.popMin(RedisQueueKeys.SCHEDULE_QUEUE, Long.MAX_VALUE)).thenReturn(Set.of());

        // Act
        this.scheduleNotificationConsumer.consume();

        // Assert
        verify(this.scheduleNotificationRepository, never()).findAllByMeetingIdAndStatus(any(), any());
    }

    @Test
    void consume_marksNotificationCompletedWhenNotifySucceeds() {
        // Arrange
        final ScheduleNotificationEntity notification = pendingNotification(1L);
        final ZSetOperations.TypedTuple<String> entry = entryOf("42");
        when(this.zSetOperations.popMin(RedisQueueKeys.SCHEDULE_QUEUE, Long.MAX_VALUE))
            .thenReturn(Set.of(entry));
        when(this.scheduleNotificationRepository.findAllByMeetingIdAndStatus(42L, NotificationStatus.PENDING))
            .thenReturn(List.of(notification));
        when(this.scheduleNotificationRepository.findWithLockById(1L)).thenReturn(Optional.of(notification));

        // Act
        this.scheduleNotificationConsumer.consume();

        // Assert
        Awaitility.await().atMost(1, TimeUnit.SECONDS)
            .until(() -> notification.getStatus() == NotificationStatus.COMPLETED);
        assertThat(notification.getLastAttemptAt()).isNotNull();
        assertThat(notification.getErrorMessage()).isNull();
    }

    @Test
    void consume_marksNotificationFailedWhenNotifyThrows() {
        // Arrange
        final ScheduleNotificationEntity notification = pendingNotification(1L);
        final ZSetOperations.TypedTuple<String> entry = entryOf("42");
        when(this.zSetOperations.popMin(RedisQueueKeys.SCHEDULE_QUEUE, Long.MAX_VALUE))
            .thenReturn(Set.of(entry));
        when(this.scheduleNotificationRepository.findAllByMeetingIdAndStatus(42L, NotificationStatus.PENDING))
            .thenReturn(List.of(notification));
        when(this.scheduleNotificationRepository.findWithLockById(1L)).thenReturn(Optional.of(notification));
        final FeignException feignException = mock(FeignException.class);
        when(feignException.getMessage()).thenReturn("connection refused");
        doThrow(feignException).when(this.fakeNotificationService).notify(any());

        // Act
        this.scheduleNotificationConsumer.consume();

        // Assert
        Awaitility.await().atMost(1, TimeUnit.SECONDS)
            .until(() -> notification.getStatus() == NotificationStatus.FAILED);
        assertThat(notification.getErrorMessage()).isEqualTo("connection refused");
        assertThat(notification.getLastAttemptAt()).isNotNull();
    }

    @Test
    void consume_abortsWhenNotificationIsNotPending() throws InterruptedException {
        // Arrange
        final ScheduleNotificationEntity notification = ScheduleNotificationEntity.builder()
            .id(1L).meeting(meeting())
            .participant(UserEntity.builder().id(2L).name("Bob").email("bob@example.com").build())
            .status(NotificationStatus.COMPLETED).build();
        final ZSetOperations.TypedTuple<String> entry = entryOf("42");
        when(this.zSetOperations.popMin(RedisQueueKeys.SCHEDULE_QUEUE, Long.MAX_VALUE))
            .thenReturn(Set.of(entry));
        when(this.scheduleNotificationRepository.findAllByMeetingIdAndStatus(42L, NotificationStatus.PENDING))
            .thenReturn(List.of(notification));
        when(this.scheduleNotificationRepository.findWithLockById(1L)).thenReturn(Optional.of(notification));

        // Act
        this.scheduleNotificationConsumer.consume();
        Thread.sleep(300);

        // Assert
        verify(this.fakeNotificationService, never()).notify(any());
    }

    @Test
    void consume_processesAllParticipantsConcurrently() {
        // Arrange
        final ScheduleNotificationEntity notif1 = pendingNotification(1L);
        final ScheduleNotificationEntity notif2 = ScheduleNotificationEntity.builder()
            .id(2L).meeting(meeting())
            .participant(UserEntity.builder().id(3L).name("Carol").email("carol@example.com").build())
            .status(NotificationStatus.PENDING).build();
        final ZSetOperations.TypedTuple<String> entry = entryOf("42");
        when(this.zSetOperations.popMin(RedisQueueKeys.SCHEDULE_QUEUE, Long.MAX_VALUE))
            .thenReturn(Set.of(entry));
        when(this.scheduleNotificationRepository.findAllByMeetingIdAndStatus(42L, NotificationStatus.PENDING))
            .thenReturn(List.of(notif1, notif2));
        when(this.scheduleNotificationRepository.findWithLockById(1L)).thenReturn(Optional.of(notif1));
        when(this.scheduleNotificationRepository.findWithLockById(2L)).thenReturn(Optional.of(notif2));

        // Act
        this.scheduleNotificationConsumer.consume();

        // Assert
        Awaitility.await().atMost(1, TimeUnit.SECONDS)
            .until(() -> notif1.getStatus() == NotificationStatus.COMPLETED
                && notif2.getStatus() == NotificationStatus.COMPLETED);
    }
}
