package org.example.meetingscheduler.timeslot;

import org.example.meetingscheduler.timeslot.dto.TimeslotResponseDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TimeslotMapper {

    TimeslotResponseDto toDto(final TimeslotEntity timeslotEntity);
}
