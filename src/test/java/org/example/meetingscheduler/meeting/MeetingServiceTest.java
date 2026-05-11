package org.example.meetingscheduler.meeting;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.example.meetingscheduler.meeting.dto.MeetingCreateRequestDto;
import org.example.meetingscheduler.meeting.dto.MeetingResponseDto;
import org.example.meetingscheduler.participant.ParticipantEntity;
import org.example.meetingscheduler.participant.ParticipantRepository;
import org.example.meetingscheduler.participant.ParticipantResponseDto;
import org.example.meetingscheduler.timeslot.SlotBookingStatus;
import org.example.meetingscheduler.timeslot.TimeslotEntity;
import org.example.meetingscheduler.timeslot.TimeslotRepository;
import org.example.meetingscheduler.user.UserEntity;
import org.example.meetingscheduler.user.UserRepository;
import org.example.meetingscheduler.user.dto.UserResponseDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MeetingServiceTest {

    @InjectMocks
    private MeetingService meetingService;
    @Mock
    private MeetingRepository meetingRepository;
    @Mock
    private MeetingMapper meetingMapper;
    @Mock
    private TimeslotRepository timeslotRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ParticipantRepository participantRepository;

    @Test
    void getMeetings_returnsEmptyList_whenRepositoryIsEmpty() {
        // Arrange
        when(this.meetingRepository.findAll()).thenReturn(Collections.emptyList());

        // Act & Assert
        assertThat(this.meetingService.getMeetings()).isEmpty();
    }

    @Test
    void getMeetings_returnsMappedDtos() {
        // Arrange
        final LocalDateTime start = LocalDateTime.of(2026, 5, 9, 10, 0);
        final LocalDateTime end = LocalDateTime.of(2026, 5, 9, 11, 0);
        final MeetingEntity meetingEntity = MeetingEntity.builder()
            .id(1L).title("Sync").startTime(start).endTime(end).participants(Collections.emptyList())
            .build();
        final MeetingResponseDto meetingResponseDto = new MeetingResponseDto(
            1L, "Sync", null, start, end,
            new UserResponseDto(1L, "Alice", "alice@example.com"), Collections.emptyList());
        when(this.meetingRepository.findAll()).thenReturn(List.of(meetingEntity));
        when(this.meetingMapper.toDto(meetingEntity)).thenReturn(meetingResponseDto);

        // Act & Assert
        assertThat(this.meetingService.getMeetings()).containsExactly(meetingResponseDto);
    }

    @Test
    void createMeeting_throwsBadRequest_whenEndTimeNotAfterStartTime() {
        // Arrange
        final LocalDateTime time = LocalDateTime.of(2026, 5, 10, 10, 0);
        final MeetingCreateRequestDto dto = new MeetingCreateRequestDto(1L, "Sync", null, time, time, List.of());

        // Act & Assert
        assertThatThrownBy(() -> this.meetingService.createMeeting(dto))
            .isInstanceOf(ResponseStatusException.class)
            .extracting(e -> ((ResponseStatusException) e).getStatusCode())
            .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void createMeeting_throwsUnprocessableEntity_whenNoAvailabilityCoversRange() {
        // Arrange
        final LocalDateTime start = LocalDateTime.of(2026, 5, 10, 10, 0);
        final LocalDateTime end = LocalDateTime.of(2026, 5, 10, 11, 0);
        when(this.timeslotRepository.findAll(ArgumentMatchers.<Specification<TimeslotEntity>>any()))
            .thenReturn(List.of());
        final MeetingCreateRequestDto dto = new MeetingCreateRequestDto(1L, "Sync", null, start, end, List.of());

        // Act & Assert
        assertThatThrownBy(() -> this.meetingService.createMeeting(dto))
            .isInstanceOf(ResponseStatusException.class)
            .extracting(e -> ((ResponseStatusException) e).getStatusCode())
            .isEqualTo(HttpStatusCode.valueOf(422));
    }

    @Test
    void createMeeting_throwsConflict_whenMeetingAlreadyExists() {
        // Arrange
        final LocalDateTime start = LocalDateTime.of(2026, 5, 10, 10, 0);
        final LocalDateTime end = LocalDateTime.of(2026, 5, 10, 11, 0);
        final UserEntity organizer = UserEntity.builder().id(1L).name("Alice").email("alice@example.com").build();
        final TimeslotEntity coveringSlot = TimeslotEntity.builder()
            .id(10L).organizer(organizer).startTime(start).endTime(end).status(SlotBookingStatus.FREE).build();
        when(this.timeslotRepository.findAll(ArgumentMatchers.<Specification<TimeslotEntity>>any()))
            .thenReturn(List.of(coveringSlot));
        when(this.meetingRepository.existsByOrganizerIdAndStartTimeAndEndTime(1L, start, end)).thenReturn(true);
        final MeetingCreateRequestDto dto = new MeetingCreateRequestDto(1L, "Sync", null, start, end, List.of());

        // Act & Assert
        assertThatThrownBy(() -> this.meetingService.createMeeting(dto))
            .isInstanceOf(ResponseStatusException.class)
            .extracting(e -> ((ResponseStatusException) e).getStatusCode())
            .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void createMeeting_throwsConflict_whenConcurrentInsertViolatesUniqueConstraint() {
        // Arrange
        final LocalDateTime start = LocalDateTime.of(2026, 5, 10, 10, 0);
        final LocalDateTime end = LocalDateTime.of(2026, 5, 10, 11, 0);
        final UserEntity organizer = UserEntity.builder().id(1L).name("Alice").email("alice@example.com").build();
        final TimeslotEntity coveringSlot = TimeslotEntity.builder()
            .id(10L).organizer(organizer).startTime(start).endTime(end).status(SlotBookingStatus.FREE).build();
        when(this.timeslotRepository.findAll(ArgumentMatchers.<Specification<TimeslotEntity>>any()))
            .thenReturn(List.of(coveringSlot));
        when(this.meetingRepository.save(any(MeetingEntity.class)))
            .thenThrow(new DataIntegrityViolationException("unique constraint"));
        final MeetingCreateRequestDto dto = new MeetingCreateRequestDto(1L, "Sync", null, start, end, List.of());

        // Act & Assert
        assertThatThrownBy(() -> this.meetingService.createMeeting(dto))
            .isInstanceOf(ResponseStatusException.class)
            .extracting(e -> ((ResponseStatusException) e).getStatusCode())
            .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void createMeeting_throwsNotFound_whenParticipantUserDoesNotExist() {
        // Arrange
        final LocalDateTime start = LocalDateTime.of(2026, 5, 10, 10, 0);
        final LocalDateTime end = LocalDateTime.of(2026, 5, 10, 11, 0);
        final UserEntity organizer = UserEntity.builder().id(1L).name("Alice").email("alice@example.com").build();
        final TimeslotEntity coveringSlot = TimeslotEntity.builder()
            .id(10L).organizer(organizer).startTime(start).endTime(end).status(SlotBookingStatus.FREE).build();
        when(this.timeslotRepository.findAll(ArgumentMatchers.<Specification<TimeslotEntity>>any()))
            .thenReturn(List.of(coveringSlot));
        when(this.meetingRepository.save(any(MeetingEntity.class)))
            .thenReturn(MeetingEntity.builder().id(100L).participants(List.of()).build());
        when(this.userRepository.findById(99L)).thenReturn(Optional.empty());
        final MeetingCreateRequestDto dto = new MeetingCreateRequestDto(1L, "Sync", null, start, end, List.of(99L));

        // Act & Assert
        assertThatThrownBy(() -> this.meetingService.createMeeting(dto))
            .isInstanceOf(ResponseStatusException.class)
            .extracting(e -> ((ResponseStatusException) e).getStatusCode())
            .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void createMeeting_createsMeetingAndParticipants_andSplitsTimeslotExactFit() {
        // Arrange — covering slot matches meeting range exactly → repurposed as BOOKED, no remainders
        final LocalDateTime start = LocalDateTime.of(2026, 5, 10, 10, 0);
        final LocalDateTime end = LocalDateTime.of(2026, 5, 10, 11, 0);
        final UserEntity organizer = UserEntity.builder().id(1L).name("Alice").email("alice@example.com").build();
        final UserEntity participantUser = UserEntity.builder().id(2L).name("Bob").email("bob@example.com").build();
        final TimeslotEntity coveringSlot = TimeslotEntity.builder()
            .id(10L).organizer(organizer).startTime(start).endTime(end).status(SlotBookingStatus.FREE).build();
        final MeetingEntity savedMeeting = MeetingEntity.builder()
            .id(100L).title("Sync").description("Weekly").startTime(start).endTime(end)
            .organizer(organizer).participants(List.of()).build();
        final ParticipantEntity savedParticipant = ParticipantEntity.builder()
            .id(1L).meeting(savedMeeting).user(participantUser).build();
        final MeetingResponseDto expected = new MeetingResponseDto(
            100L, "Sync", "Weekly", start, end,
            new UserResponseDto(1L, "Alice", "alice@example.com"),
            List.of(new ParticipantResponseDto(1L, new UserResponseDto(2L, "Bob", "bob@example.com"))));
        when(this.timeslotRepository.findAll(ArgumentMatchers.<Specification<TimeslotEntity>>any()))
            .thenReturn(List.of(coveringSlot));
        when(this.meetingRepository.save(any(MeetingEntity.class))).thenReturn(savedMeeting);
        when(this.userRepository.findById(2L)).thenReturn(Optional.of(participantUser));
        when(this.participantRepository.saveAll(any())).thenReturn(List.of(savedParticipant));
        when(this.meetingMapper.toDto(savedMeeting)).thenReturn(expected);
        final MeetingCreateRequestDto dto = new MeetingCreateRequestDto(1L, "Sync", "Weekly", start, end, List.of(2L));

        // Act
        final MeetingResponseDto result = this.meetingService.createMeeting(dto);

        // Assert
        assertThat(result).isEqualTo(expected);
        assertThat(coveringSlot.getStatus()).isEqualTo(SlotBookingStatus.BOOKED);
        assertThat(coveringSlot.getEndTime()).isEqualTo(end);
    }

    @Test
    void createMeeting_splitsTimeslot_whenMeetingStartsAfterSlotStart() {
        // Arrange — slot 09:00-11:00, meeting 10:00-11:00 → left remainder [09:00,10:00] FREE, existing → BOOKED [10:00,11:00]
        final LocalDateTime slotStart = LocalDateTime.of(2026, 5, 10, 9, 0);
        final LocalDateTime meetingStart = LocalDateTime.of(2026, 5, 10, 10, 0);
        final LocalDateTime end = LocalDateTime.of(2026, 5, 10, 11, 0);
        final UserEntity organizer = UserEntity.builder().id(1L).name("Alice").email("alice@example.com").build();
        final TimeslotEntity coveringSlot = TimeslotEntity.builder()
            .id(10L).organizer(organizer).startTime(slotStart).endTime(end).status(SlotBookingStatus.FREE).build();
        final MeetingEntity savedMeeting = MeetingEntity.builder()
            .id(100L).title("Sync").startTime(meetingStart).endTime(end)
            .organizer(organizer).participants(List.of()).build();
        final MeetingResponseDto expected = new MeetingResponseDto(
            100L, "Sync", null, meetingStart, end,
            new UserResponseDto(1L, "Alice", "alice@example.com"), List.of());
        when(this.timeslotRepository.findAll(ArgumentMatchers.<Specification<TimeslotEntity>>any()))
            .thenReturn(List.of(coveringSlot));
        when(this.timeslotRepository.save(any(TimeslotEntity.class))).thenReturn(TimeslotEntity.builder().build());
        when(this.meetingRepository.save(any(MeetingEntity.class))).thenReturn(savedMeeting);
        when(this.participantRepository.saveAll(any())).thenReturn(List.of());
        when(this.meetingMapper.toDto(savedMeeting)).thenReturn(expected);
        final MeetingCreateRequestDto dto = new MeetingCreateRequestDto(1L, "Sync", null, meetingStart, end, List.of());

        // Act
        final MeetingResponseDto result = this.meetingService.createMeeting(dto);

        // Assert
        assertThat(result).isEqualTo(expected);
        // existing slot shrunk to left remainder
        assertThat(coveringSlot.getEndTime()).isEqualTo(meetingStart);
        assertThat(coveringSlot.getStatus()).isEqualTo(SlotBookingStatus.FREE);
    }

    @Test
    void createMeeting_splitsTimeslot_whenMeetingEndsBeforeSlotEnd() {
        // Arrange — slot 10:00-12:00, meeting 10:00-11:00 → existing → BOOKED [10:00,11:00], right remainder [11:00,12:00] FREE
        final LocalDateTime start = LocalDateTime.of(2026, 5, 10, 10, 0);
        final LocalDateTime meetingEnd = LocalDateTime.of(2026, 5, 10, 11, 0);
        final LocalDateTime slotEnd = LocalDateTime.of(2026, 5, 10, 12, 0);
        final UserEntity organizer = UserEntity.builder().id(1L).name("Alice").email("alice@example.com").build();
        final TimeslotEntity coveringSlot = TimeslotEntity.builder()
            .id(10L).organizer(organizer).startTime(start).endTime(slotEnd).status(SlotBookingStatus.FREE).build();
        final MeetingEntity savedMeeting = MeetingEntity.builder()
            .id(100L).title("Sync").startTime(start).endTime(meetingEnd)
            .organizer(organizer).participants(List.of()).build();
        final MeetingResponseDto expected = new MeetingResponseDto(
            100L, "Sync", null, start, meetingEnd,
            new UserResponseDto(1L, "Alice", "alice@example.com"), List.of());
        when(this.timeslotRepository.findAll(ArgumentMatchers.<Specification<TimeslotEntity>>any()))
            .thenReturn(List.of(coveringSlot));
        when(this.timeslotRepository.save(any(TimeslotEntity.class))).thenReturn(TimeslotEntity.builder().build());
        when(this.meetingRepository.save(any(MeetingEntity.class))).thenReturn(savedMeeting);
        when(this.participantRepository.saveAll(any())).thenReturn(List.of());
        when(this.meetingMapper.toDto(savedMeeting)).thenReturn(expected);
        final MeetingCreateRequestDto dto = new MeetingCreateRequestDto(1L, "Sync", null, start, meetingEnd, List.of());

        // Act
        final MeetingResponseDto result = this.meetingService.createMeeting(dto);

        // Assert
        assertThat(result).isEqualTo(expected);
        assertThat(coveringSlot.getStatus()).isEqualTo(SlotBookingStatus.BOOKED);
        assertThat(coveringSlot.getEndTime()).isEqualTo(meetingEnd);
    }
}
