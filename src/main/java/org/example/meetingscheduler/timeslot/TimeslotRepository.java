package org.example.meetingscheduler.timeslot;

import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TimeslotRepository extends JpaRepository<TimeslotEntity, Long>, JpaSpecificationExecutor<TimeslotEntity> {

    Optional<TimeslotEntity> findByOrganizerIdAndStartTimeAndEndTime(final Long organizerId,
                                                                     final LocalDateTime startTime,
                                                                     final LocalDateTime endTime);
}
