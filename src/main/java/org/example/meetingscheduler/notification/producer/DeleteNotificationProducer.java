package org.example.meetingscheduler.notification.producer;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.meetingscheduler.notification.dto.NotificationStatus;
import org.example.meetingscheduler.notification.entity.DeleteNotificationEntity;
import org.example.meetingscheduler.notification.repository.DeleteNotificationRepository;
import org.example.meetingscheduler.notification.service.NotificationEnqueueService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeleteNotificationProducer {

    private final DeleteNotificationRepository deleteNotificationRepository;
    private final NotificationEnqueueService notificationEnqueueService;

    @Scheduled(fixedDelay = 10000)
    public void produce() {
        final List<Long> pendingMeetingIds = this.deleteNotificationRepository.findAllByStatus(NotificationStatus.PENDING)
            .stream()
            .map(DeleteNotificationEntity::getMeetingId)
            .distinct()
            .toList();
        log.debug("Found {} pending delete notification(s)", pendingMeetingIds.size());
        if (pendingMeetingIds.isEmpty()) {
            return;
        }
        log.info("Re-enqueuing {} meeting(s) with pending delete notifications", pendingMeetingIds.size());
        pendingMeetingIds.forEach(this.notificationEnqueueService::enqueueDelete);
    }
}
