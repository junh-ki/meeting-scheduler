package org.example.meetingscheduler.notification.producer;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.meetingscheduler.notification.dto.NotificationStatus;
import org.example.meetingscheduler.notification.repository.ScheduleNotificationRepository;
import org.example.meetingscheduler.notification.service.NotificationEnqueueService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduleNotificationProducer {

    private final ScheduleNotificationRepository scheduleNotificationRepository;
    private final NotificationEnqueueService notificationEnqueueService;

    @Scheduled(fixedDelay = 10000)
    public void produce() {
        final List<Long> pendingMeetingIds = this.scheduleNotificationRepository.findAllByStatus(NotificationStatus.PENDING)
            .stream()
            .map(scheduleNotificationEntity -> scheduleNotificationEntity.getMeeting().getId())
            .distinct()
            .toList();
        log.debug("Found {} pending schedule notification(s)", pendingMeetingIds.size());
        if (pendingMeetingIds.isEmpty()) {
            return;
        }
        log.info("Re-enqueuing {} meeting(s) with pending schedule notifications", pendingMeetingIds.size());
        pendingMeetingIds.forEach(this.notificationEnqueueService::enqueueSchedule);
    }
}
