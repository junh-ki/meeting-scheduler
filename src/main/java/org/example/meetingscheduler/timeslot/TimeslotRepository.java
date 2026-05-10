package org.example.meetingscheduler.timeslot;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TimeslotRepository extends JpaRepository<TimeslotEntity, Long>, JpaSpecificationExecutor<TimeslotEntity> {}
