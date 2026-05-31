package org.example.meetingscheduler.notification.producer;

import java.util.List;
import org.example.meetingscheduler.notification.dto.NotificationStatus;
import org.example.meetingscheduler.notification.entity.DeleteNotificationEntity;
import org.example.meetingscheduler.notification.repository.DeleteNotificationRepository;
import org.example.meetingscheduler.notification.service.NotificationEnqueueService;
import org.example.meetingscheduler.user.UserEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeleteNotificationProducerTest {

    @InjectMocks
    private DeleteNotificationProducer deleteNotificationProducer;
    @Mock
    private DeleteNotificationRepository deleteNotificationRepository;
    @Mock
    private NotificationEnqueueService notificationEnqueueService;

    @Test
    void produce_enqueuesMeetingIdForEachPendingNotification() {
        // Arrange
        when(this.deleteNotificationRepository.findAllByStatus(NotificationStatus.PENDING))
            .thenReturn(List.of(notificationFor(1L, 42L)));

        // Act
        this.deleteNotificationProducer.produce();

        // Assert
        verify(this.notificationEnqueueService).enqueueDelete(42L);
    }

    @Test
    void produce_doesNothingWhenNoPendingNotifications() {
        // Arrange
        when(this.deleteNotificationRepository.findAllByStatus(NotificationStatus.PENDING))
            .thenReturn(List.of());

        // Act
        this.deleteNotificationProducer.produce();

        // Assert
        verify(this.notificationEnqueueService, never()).enqueueDelete(anyLong());
    }

    @Test
    void produce_deduplicatesWhenMultipleNotificationsShareTheSameMeeting() {
        // Arrange
        when(this.deleteNotificationRepository.findAllByStatus(NotificationStatus.PENDING))
            .thenReturn(List.of(notificationFor(1L, 42L), notificationFor(2L, 42L)));

        // Act
        this.deleteNotificationProducer.produce();

        // Assert
        verify(this.notificationEnqueueService, times(1)).enqueueDelete(42L);
    }

    private DeleteNotificationEntity notificationFor(final long id,
                                                     final long meetingId) {
        return DeleteNotificationEntity.builder()
            .id(id).meetingId(meetingId).meetingTitle("Sync")
            .participant(UserEntity.builder().id(2L).name("Bob").email("bob@example.com").build())
            .status(NotificationStatus.PENDING).build();
    }
}
