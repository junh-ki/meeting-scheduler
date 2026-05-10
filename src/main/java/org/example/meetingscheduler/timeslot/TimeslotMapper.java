package org.example.meetingscheduler.timeslot;

import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TimeslotMapper {

    TimeslotResponseDto toDto(final TimeslotEntity timeslotEntity);
}
