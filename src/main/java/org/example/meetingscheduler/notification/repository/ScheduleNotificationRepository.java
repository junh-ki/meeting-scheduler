package org.example.meetingscheduler.notification.repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.example.meetingscheduler.notification.dto.NotificationStatus;
import org.example.meetingscheduler.notification.entity.ScheduleNotificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface ScheduleNotificationRepository extends JpaRepository<ScheduleNotificationEntity, Long> {

    List<ScheduleNotificationEntity> findAllByMeetingIdAndStatus(final Long meetingId,
                                                                 final NotificationStatus notificationStatus);

    List<ScheduleNotificationEntity> findAllByStatus(final NotificationStatus notificationStatus);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<ScheduleNotificationEntity> findWithLockById(final Long id);
}
