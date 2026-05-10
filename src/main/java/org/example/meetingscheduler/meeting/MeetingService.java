package org.example.meetingscheduler.meeting;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.example.meetingscheduler.meeting.dto.MeetingCreateRequestDto;
import org.example.meetingscheduler.meeting.dto.MeetingResponseDto;
import org.example.meetingscheduler.participant.ParticipantEntity;
import org.example.meetingscheduler.participant.ParticipantRepository;
import org.example.meetingscheduler.timeslot.SlotBookingStatus;
import org.example.meetingscheduler.timeslot.TimeslotEntity;
import org.example.meetingscheduler.timeslot.TimeslotRepository;
import org.example.meetingscheduler.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class MeetingService {

    private final MeetingRepository meetingRepository;
    private final MeetingMapper meetingMapper;
    private final TimeslotRepository timeslotRepository;
    private final UserRepository userRepository;
    private final ParticipantRepository participantRepository;

    public List<MeetingResponseDto> getMeetings() {
        return this.meetingRepository.findAll().stream()
            .map(this.meetingMapper::toDto)
            .toList();
    }

    @Transactional
    public MeetingResponseDto createMeeting(final MeetingCreateRequestDto meetingCreateRequestDto) {
        final TimeslotEntity timeslotEntity = this.timeslotRepository.findById(meetingCreateRequestDto.timeslotId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Timeslot not found"));
        if (SlotBookingStatus.BOOKED.equals(timeslotEntity.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Timeslot is not available");
        }
        timeslotEntity.setStatus(SlotBookingStatus.BOOKED);
        return this.meetingMapper.toDto(
            createMeetingWithParticipants(
                meetingCreateRequestDto,
                timeslotEntity
            )
        );
    }

    private MeetingEntity createMeetingWithParticipants(final MeetingCreateRequestDto meetingCreateRequestDto,
                                                        final TimeslotEntity timeslotEntity) {
        final MeetingEntity meetingEntity = this.meetingRepository.save(
            MeetingEntity.builder()
                .title(meetingCreateRequestDto.title())
                .description(meetingCreateRequestDto.description())
                .startTime(timeslotEntity.getStartTime())
                .endTime(timeslotEntity.getEndTime())
                .organizer(timeslotEntity.getOrganizer())
                .participants(List.of())
                .build()
        );
        final List<ParticipantEntity> participants = meetingCreateRequestDto.participantUserIds().stream()
            .map(userId ->
                this.userRepository.findById(userId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + userId))
            )
            .map(userEntity ->
                ParticipantEntity.builder()
                    .meeting(meetingEntity)
                    .user(userEntity)
                    .build()
            )
            .toList();
        meetingEntity.setParticipants(this.participantRepository.saveAll(participants));
        return meetingEntity;
    }
}
