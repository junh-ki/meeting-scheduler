package org.example.meetingscheduler.meeting;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.meetingscheduler.meeting.dto.MeetingCreateRequestDto;
import org.example.meetingscheduler.meeting.dto.MeetingResponseDto;
import org.example.meetingscheduler.participant.ParticipantEntity;
import org.example.meetingscheduler.participant.ParticipantRepository;
import org.example.meetingscheduler.timeslot.SlotBookingStatus;
import org.example.meetingscheduler.timeslot.TimeslotEntity;
import org.example.meetingscheduler.timeslot.TimeslotRepository;
import org.example.meetingscheduler.timeslot.TimeslotSpecifications;
import org.example.meetingscheduler.user.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
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
        final LocalDateTime startTime = meetingCreateRequestDto.startTime();
        final LocalDateTime endTime = meetingCreateRequestDto.endTime();
        if (!endTime.isAfter(startTime)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "endTime must be after startTime");
        }
        final TimeslotEntity coveringSlot = this.timeslotRepository.findAll(
            TimeslotSpecifications
                .hasOwnerId(meetingCreateRequestDto.organizerId())
                .and(TimeslotSpecifications.hasStatus(SlotBookingStatus.FREE))
                .and(TimeslotSpecifications.coversRange(startTime, endTime))
            ).stream()
            .findFirst()
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatusCode.valueOf(422), "No available timeslot covers the requested range")
            );
        if (this.meetingRepository.existsByOrganizerIdAndStartTimeAndEndTime(
            meetingCreateRequestDto.organizerId(),
            startTime,
            endTime)) {
            log.warn(
                "Duplicate meeting creation rejected: organizerId={}, startTime={}, endTime={}",
                meetingCreateRequestDto.organizerId(),
                startTime,
                endTime
            );
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Meeting already exists for this time range"
            );
        }
        try {
            return this.meetingMapper.toDto(
                createMeetingWithParticipants(
                    meetingCreateRequestDto,
                    coveringSlot
                )
            );
        } catch (final DataIntegrityViolationException dataIntegrityViolationException) {
            log.warn(
                "Concurrent duplicate meeting creation rejected: organizerId={}, startTime={}, endTime={}",
                meetingCreateRequestDto.organizerId(),
                startTime,
                endTime
            );
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Meeting already exists for this time range"
            );
        }
    }

    private MeetingEntity createMeetingWithParticipants(final MeetingCreateRequestDto meetingCreateRequestDto,
                                                        final TimeslotEntity coveringSlot) {
        mutateTimeslot(coveringSlot, meetingCreateRequestDto.startTime(), meetingCreateRequestDto.endTime());
        final MeetingEntity meetingEntity = this.meetingRepository.save(
            MeetingEntity.builder()
                .title(meetingCreateRequestDto.title())
                .description(meetingCreateRequestDto.description())
                .startTime(meetingCreateRequestDto.startTime())
                .endTime(meetingCreateRequestDto.endTime())
                .organizer(coveringSlot.getOwner())
                .participants(new ArrayList<>())
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

    private void mutateTimeslot(final TimeslotEntity timeslotEntity,
                                final LocalDateTime meetingStart,
                                final LocalDateTime meetingEnd) {
        final LocalDateTime originalEnd = timeslotEntity.getEndTime();
        if (meetingStart.isAfter(timeslotEntity.getStartTime())) {
            timeslotEntity.setEndTime(meetingStart); // Left remainder: shrink existing slot to [slot.start, meetingStart] FREE
            this.timeslotRepository.save(
                TimeslotEntity.builder()
                    .owner(timeslotEntity.getOwner())
                    .startTime(meetingStart)
                    .endTime(meetingEnd)
                    .status(SlotBookingStatus.BOOKED) // BOOKED slot covering meeting range
                    .build()
            );
        } else {
            timeslotEntity.setEndTime(meetingEnd); // No left remainder: repurpose existing slot as the BOOKED slot
            timeslotEntity.setStatus(SlotBookingStatus.BOOKED);
        }
        if (meetingEnd.isBefore(originalEnd)) {
            this.timeslotRepository.save(
                TimeslotEntity.builder() // Right remainder: new FREE slot for [meetingEnd, originalEnd]
                    .owner(timeslotEntity.getOwner())
                    .startTime(meetingEnd)
                    .endTime(originalEnd)
                    .status(SlotBookingStatus.FREE)
                    .build()
            );
        }
    }
}
