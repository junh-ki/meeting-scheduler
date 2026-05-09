package org.example.meetingscheduler.meeting;

import java.util.List;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MeetingRepository extends JpaRepository<MeetingEntity, Long> {

    @NullMarked
    @EntityGraph(attributePaths = {"organizer", "participants", "participants.user"})
    List<MeetingEntity> findAll();
}
