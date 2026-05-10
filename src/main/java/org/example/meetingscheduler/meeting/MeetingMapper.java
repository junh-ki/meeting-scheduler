package org.example.meetingscheduler.meeting;

import org.example.meetingscheduler.meeting.dto.MeetingResponseDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface MeetingMapper {

    MeetingResponseDto toDto(final MeetingEntity meetingEntity);
}
