package org.example.meetingscheduler.timeslot;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;

public interface TimeslotRepository extends JpaRepository<TimeslotEntity, Long>, JpaSpecificationExecutor<TimeslotEntity> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<TimeslotEntity> findWithLockById(final Long id);

    Optional<TimeslotEntity> findByOwnerIdAndStartTimeAndEndTimeAndStatus(final Long ownerId,
                                                                          final LocalDateTime startTime,
                                                                          final LocalDateTime endTime,
                                                                          final SlotBookingStatus status);
}
