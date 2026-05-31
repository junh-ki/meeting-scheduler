package org.example.meetingscheduler.notification.consumer;

import feign.FeignException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.awaitility.Awaitility;
import org.example.meetingscheduler.notification.dto.NotificationStatus;
import org.example.meetingscheduler.notification.entity.DeleteNotificationEntity;
import org.example.meetingscheduler.notification.repository.DeleteNotificationRepository;
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
class DeleteNotificationConsumerTest {

    private static final LocalDateTime START = LocalDateTime.of(2026, 5, 10, 10, 0);
    private static final LocalDateTime END = LocalDateTime.of(2026, 5, 10, 11, 0);
    @InjectMocks
    private DeleteNotificationConsumer consumer;
    @Mock
    private RedisTemplate<String, String> redisTemplate;
    @Mock
    private ZSetOperations<String, String> zSetOperations;
    @Mock
    private DeleteNotificationRepository deleteNotificationRepository;
    @Mock
    private FakeNotificationService fakeNotificationService;
    @Mock
    private PlatformTransactionManager platformTransactionManager;

    @BeforeEach
    void setUp() {
        when(this.redisTemplate.opsForZSet()).thenReturn(this.zSetOperations);
        lenient().when(this.platformTransactionManager.getTransaction(any())).thenReturn(mock(TransactionStatus.class));
    }

    @Test
    void consume_doesNothingWhenQueueIsEmpty() {
        // Arrange
        when(this.zSetOperations.popMin(RedisQueueKeys.DELETE_QUEUE, Long.MAX_VALUE)).thenReturn(Set.of());

        // Act
        this.consumer.consume();

        // Assert
        verify(this.deleteNotificationRepository, never()).findAllByMeetingIdAndStatus(any(), any());
    }

    @Test
    void consume_marksNotificationCompletedWhenNotifySucceeds() {
        // Arrange
        final DeleteNotificationEntity notification = pendingNotification(1L);
        final ZSetOperations.TypedTuple<String> entry = entryOf("42");
        when(this.zSetOperations.popMin(RedisQueueKeys.DELETE_QUEUE, Long.MAX_VALUE))
            .thenReturn(Set.of(entry));
        when(this.deleteNotificationRepository.findAllByMeetingIdAndStatus(42L, NotificationStatus.PENDING))
            .thenReturn(List.of(notification));
        when(this.deleteNotificationRepository.findWithLockById(1L)).thenReturn(Optional.of(notification));

        // Act
        this.consumer.consume();

        // Assert
        Awaitility.await().atMost(1, TimeUnit.SECONDS)
            .until(() -> notification.getStatus() == NotificationStatus.COMPLETED);
        assertThat(notification.getLastAttemptAt()).isNotNull();
        assertThat(notification.getErrorMessage()).isNull();
    }

    @Test
    void consume_marksNotificationFailedWhenNotifyThrows() {
        // Arrange
        final DeleteNotificationEntity notification = pendingNotification(1L);
        final ZSetOperations.TypedTuple<String> entry = entryOf("42");
        when(this.zSetOperations.popMin(RedisQueueKeys.DELETE_QUEUE, Long.MAX_VALUE))
            .thenReturn(Set.of(entry));
        when(this.deleteNotificationRepository.findAllByMeetingIdAndStatus(42L, NotificationStatus.PENDING))
            .thenReturn(List.of(notification));
        when(this.deleteNotificationRepository.findWithLockById(1L)).thenReturn(Optional.of(notification));
        final FeignException feignException = mock(FeignException.class);
        when(feignException.getMessage()).thenReturn("timeout");
        doThrow(feignException).when(this.fakeNotificationService).notify(any());

        // Act
        this.consumer.consume();

        // Assert
        Awaitility.await().atMost(1, TimeUnit.SECONDS)
            .until(() -> notification.getStatus() == NotificationStatus.FAILED);
        assertThat(notification.getErrorMessage()).isEqualTo("timeout");
        assertThat(notification.getLastAttemptAt()).isNotNull();
    }

    @Test
    void consume_abortsWhenNotificationIsNotPending() throws InterruptedException {
        // Arrange
        final DeleteNotificationEntity notification = DeleteNotificationEntity.builder()
            .id(1L).meetingId(42L).meetingTitle("Sync")
            .participant(UserEntity.builder().id(2L).name("Bob").email("bob@example.com").build())
            .status(NotificationStatus.COMPLETED).build();
        final ZSetOperations.TypedTuple<String> entry = entryOf("42");
        when(this.zSetOperations.popMin(RedisQueueKeys.DELETE_QUEUE, Long.MAX_VALUE))
            .thenReturn(Set.of(entry));
        when(this.deleteNotificationRepository.findAllByMeetingIdAndStatus(42L, NotificationStatus.PENDING))
            .thenReturn(List.of(notification));
        when(this.deleteNotificationRepository.findWithLockById(1L)).thenReturn(Optional.of(notification));

        // Act
        this.consumer.consume();
        Thread.sleep(300);

        // Assert
        verify(this.fakeNotificationService, never()).notify(any());
    }

    @SuppressWarnings("unchecked")
    private ZSetOperations.TypedTuple<String> entryOf(final String value) {
        final ZSetOperations.TypedTuple<String> entry = mock(ZSetOperations.TypedTuple.class);
        when(entry.getValue()).thenReturn(value);
        return entry;
    }

    private DeleteNotificationEntity pendingNotification(final long id) {
        return DeleteNotificationEntity.builder()
            .id(id).meetingId(42L).meetingTitle("Sync")
            .meetingStartTime(START).meetingEndTime(END)
            .participant(UserEntity.builder().id(2L).name("Bob").email("bob@example.com").build())
            .status(NotificationStatus.PENDING).build();
    }
}
