package org.example.meetingscheduler.timeslot;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TimeslotRepository extends JpaRepository<TimeslotEntity, Long> {}
