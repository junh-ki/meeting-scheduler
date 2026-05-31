package org.example.meetingscheduler.notification.producer;

import java.time.LocalDateTime;
import java.util.List;
import org.example.meetingscheduler.meeting.MeetingEntity;
import org.example.meetingscheduler.notification.dto.NotificationStatus;
import org.example.meetingscheduler.notification.entity.ScheduleNotificationEntity;
import org.example.meetingscheduler.notification.repository.ScheduleNotificationRepository;
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
class ScheduleNotificationProducerTest {

    @InjectMocks
    private ScheduleNotificationProducer scheduleNotificationProducer;
    @Mock
    private ScheduleNotificationRepository scheduleNotificationRepository;
    @Mock
    private NotificationEnqueueService notificationEnqueueService;

    @Test
    void produce_enqueuesMeetingIdForEachPendingNotification() {
        // Arrange
        final MeetingEntity meeting = meetingOf(42L);
        when(this.scheduleNotificationRepository.findAllByStatus(NotificationStatus.PENDING))
            .thenReturn(List.of(notificationFor(1L, meeting)));

        // Act
        this.scheduleNotificationProducer.produce();

        // Assert
        verify(this.notificationEnqueueService).enqueueSchedule(42L);
    }

    @Test
    void produce_doesNothingWhenNoPendingNotifications() {
        // Arrange
        when(this.scheduleNotificationRepository.findAllByStatus(NotificationStatus.PENDING))
            .thenReturn(List.of());

        // Act
        this.scheduleNotificationProducer.produce();

        // Assert
        verify(this.notificationEnqueueService, never()).enqueueSchedule(anyLong());
    }

    @Test
    void produce_deduplicatesWhenMultipleNotificationsShareTheSameMeeting() {
        // Arrange
        final MeetingEntity meeting = meetingOf(42L);
        when(this.scheduleNotificationRepository.findAllByStatus(NotificationStatus.PENDING))
            .thenReturn(List.of(notificationFor(1L, meeting), notificationFor(2L, meeting)));

        // Act
        this.scheduleNotificationProducer.produce();

        // Assert
        verify(this.notificationEnqueueService, times(1)).enqueueSchedule(42L);
    }

    private MeetingEntity meetingOf(final long id) {
        return MeetingEntity.builder()
            .id(id).title("Sync")
            .startTime(LocalDateTime.of(2026, 5, 10, 10, 0))
            .endTime(LocalDateTime.of(2026, 5, 10, 11, 0))
            .organizer(UserEntity.builder().id(99L).name("Alice").email("alice@example.com").build())
            .participants(List.of())
            .build();
    }

    private ScheduleNotificationEntity notificationFor(final long id, final MeetingEntity meeting) {
        return ScheduleNotificationEntity.builder()
            .id(id).meeting(meeting)
            .participant(UserEntity.builder().id(2L).name("Bob").email("bob@example.com").build())
            .status(NotificationStatus.PENDING).build();
    }
}
