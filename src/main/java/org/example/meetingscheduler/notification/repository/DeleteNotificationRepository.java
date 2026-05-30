package org.example.meetingscheduler.notification.repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.example.meetingscheduler.notification.dto.NotificationStatus;
import org.example.meetingscheduler.notification.entity.DeleteNotificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface DeleteNotificationRepository extends JpaRepository<DeleteNotificationEntity, Long> {

    List<DeleteNotificationEntity> findAllByMeetingIdAndStatus(final Long meetingId,
                                                               final NotificationStatus notificationStatus);

    List<DeleteNotificationEntity> findAllByStatus(final NotificationStatus notificationStatus);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<DeleteNotificationEntity> findWithLockById(final Long id);
}
