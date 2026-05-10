package org.example.meetingscheduler.meeting;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
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
    void createMeeting_createsMeetingAndParticipants_andBooksTimeslot() {
        // Arrange
        final LocalDateTime start = LocalDateTime.of(2026, 5, 10, 10, 0);
        final LocalDateTime end = LocalDateTime.of(2026, 5, 10, 11, 0);
        final UserEntity organizer = UserEntity.builder().id(1L).name("Alice").email("alice@example.com").build();
        final UserEntity participantUser = UserEntity.builder().id(2L).name("Bob").email("bob@example.com").build();
        final TimeslotEntity timeslotEntity = TimeslotEntity.builder()
            .id(10L).organizer(organizer).startTime(start).endTime(end).status(SlotBookingStatus.FREE).build();
        final MeetingEntity savedMeeting = MeetingEntity.builder()
            .id(100L).title("Sync").description("Weekly").startTime(start).endTime(end)
            .organizer(organizer).participants(List.of()).build();
        final ParticipantEntity savedParticipant = ParticipantEntity.builder()
            .id(1L).meeting(savedMeeting).user(participantUser).build();
        final MeetingResponseDto expectedMeetingResponseDto = new MeetingResponseDto(
            100L, "Sync", "Weekly", start, end,
            new UserResponseDto(1L, "Alice", "alice@example.com"),
            List.of(new ParticipantResponseDto(1L, new UserResponseDto(2L, "Bob", "bob@example.com"))));
        when(this.timeslotRepository.findById(10L)).thenReturn(Optional.of(timeslotEntity));
        when(this.meetingRepository.save(any(MeetingEntity.class))).thenReturn(savedMeeting);
        when(this.userRepository.findById(2L)).thenReturn(Optional.of(participantUser));
        when(this.participantRepository.saveAll(any())).thenReturn(List.of(savedParticipant));
        when(this.meetingMapper.toDto(savedMeeting)).thenReturn(expectedMeetingResponseDto);
        final MeetingCreateRequestDto meetingCreateRequestDto = new MeetingCreateRequestDto(10L, "Sync", "Weekly", List.of(2L));

        // Act
        final MeetingResponseDto meetingResponseDto = this.meetingService.createMeeting(meetingCreateRequestDto);

        // Assert
        assertThat(meetingResponseDto).isEqualTo(expectedMeetingResponseDto);
        assertThat(timeslotEntity.getStatus()).isEqualTo(SlotBookingStatus.BOOKED);
    }

    @Test
    void createMeeting_throwsNotFound_whenTimeslotDoesNotExist() {
        // Arrange
        when(this.timeslotRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> this.meetingService.createMeeting(
                new MeetingCreateRequestDto(99L, "Sync", null, List.of())))
            .isInstanceOf(ResponseStatusException.class)
            .extracting(e -> ((ResponseStatusException) e).getStatusCode())
            .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void createMeeting_throwsConflict_whenTimeslotIsNotFree() {
        // Arrange
        final TimeslotEntity booked = TimeslotEntity.builder()
            .id(10L).status(SlotBookingStatus.BOOKED)
            .startTime(LocalDateTime.of(2026, 5, 10, 10, 0))
            .endTime(LocalDateTime.of(2026, 5, 10, 11, 0)).build();
        when(this.timeslotRepository.findById(10L)).thenReturn(Optional.of(booked));

        // Act & Assert
        assertThatThrownBy(() -> this.meetingService.createMeeting(
                new MeetingCreateRequestDto(10L, "Sync", null, List.of())))
            .isInstanceOf(ResponseStatusException.class)
            .extracting(e -> ((ResponseStatusException) e).getStatusCode())
            .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void createMeeting_throwsNotFound_whenParticipantUserDoesNotExist() {
        // Arrange
        final UserEntity organizer = UserEntity.builder().id(1L).name("Alice").email("alice@example.com").build();
        final TimeslotEntity timeslotEntity = TimeslotEntity.builder()
            .id(10L).organizer(organizer).status(SlotBookingStatus.FREE)
            .startTime(LocalDateTime.of(2026, 5, 10, 10, 0))
            .endTime(LocalDateTime.of(2026, 5, 10, 11, 0)).build();
        when(this.timeslotRepository.findById(10L)).thenReturn(Optional.of(timeslotEntity));
        when(this.meetingRepository.save(any(MeetingEntity.class))).thenReturn(
            MeetingEntity.builder().id(100L).participants(List.of()).build());
        when(this.userRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> this.meetingService.createMeeting(
                new MeetingCreateRequestDto(10L, "Sync", null, List.of(99L))))
            .isInstanceOf(ResponseStatusException.class)
            .extracting(e -> ((ResponseStatusException) e).getStatusCode())
            .isEqualTo(HttpStatus.NOT_FOUND);
    }
}
