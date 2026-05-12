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
import org.example.meetingscheduler.timeslot.TimeslotService;
import org.example.meetingscheduler.user.UserEntity;
import org.example.meetingscheduler.user.dto.UserResponseDto;
import org.junit.jupiter.api.Nested;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
    private TimeslotService timeslotService;
    @Mock
    private ParticipantRepository participantRepository;

    @Nested
    class getMeetings {

        @Test
        void returnsEmptyList_whenRepositoryIsEmpty() {
            // Arrange
            when(meetingRepository.findAllByOrderByStartTimeAscEndTimeAsc())
                .thenReturn(Collections.emptyList());

            // Act & Assert
            assertThat(meetingService.getMeetings()).isEmpty();
        }

        @Test
        void returnsMappedDtos() {
            // Arrange
            final LocalDateTime start = LocalDateTime.of(2026, 5, 9, 10, 0);
            final LocalDateTime end = LocalDateTime.of(2026, 5, 9, 11, 0);
            final MeetingEntity meetingEntity = MeetingEntity.builder()
                .id(1L).title("Sync").startTime(start).endTime(end).participants(Collections.emptyList())
                .build();
            final MeetingResponseDto meetingResponseDto = new MeetingResponseDto(
                1L, "Sync", null, start, end,
                new UserResponseDto(1L, "Alice", "alice@example.com"), Collections.emptyList());
            when(meetingRepository.findAllByOrderByStartTimeAscEndTimeAsc())
                .thenReturn(List.of(meetingEntity));
            when(meetingMapper.toDto(meetingEntity))
                .thenReturn(meetingResponseDto);

            // Act & Assert
            assertThat(meetingService.getMeetings()).containsExactly(meetingResponseDto);
        }
    }

    @Nested
    class createMeeting {

        @Test
        void throwsBadRequest_whenEndTimeNotAfterStartTime() {
            // Arrange
            final LocalDateTime time = LocalDateTime.of(2026, 5, 10, 10, 0);
            final MeetingCreateRequestDto meetingCreateRequestDto = new MeetingCreateRequestDto(1L, "Sync", null, time, time, List.of());

            // Act & Assert
            assertThatThrownBy(() -> meetingService.createMeeting(meetingCreateRequestDto))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        void throwsBadRequest_whenStartAndEndTimeSpanMidnight() {
            // Arrange
            final LocalDateTime start = LocalDateTime.of(2026, 5, 10, 23, 30);
            final LocalDateTime end = LocalDateTime.of(2026, 5, 11, 0, 30);
            final MeetingCreateRequestDto meetingCreateRequestDto = new MeetingCreateRequestDto(1L, "Sync", null, start, end, List.of());

            // Act & Assert
            assertThatThrownBy(() -> meetingService.createMeeting(meetingCreateRequestDto))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        void throwsUnprocessableEntity_whenOrganizerHasNoAvailability() {
            // Arrange
            final LocalDateTime start = LocalDateTime.of(2026, 5, 10, 10, 0);
            final LocalDateTime end = LocalDateTime.of(2026, 5, 10, 11, 0);
            when(timeslotRepository.findAll(ArgumentMatchers.<Specification<TimeslotEntity>>any()))
                .thenReturn(List.of());
            final MeetingCreateRequestDto meetingCreateRequestDto = new MeetingCreateRequestDto(1L, "Sync", null, start, end, List.of());

            // Act & Assert
            assertThatThrownBy(() -> meetingService.createMeeting(meetingCreateRequestDto))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatusCode.valueOf(422));
        }

        @Test
        void throwsUnprocessableEntity_whenParticipantHasNoAvailability() {
            // Arrange
            final LocalDateTime start = LocalDateTime.of(2026, 5, 10, 10, 0);
            final LocalDateTime end = LocalDateTime.of(2026, 5, 10, 11, 0);
            final UserEntity organizer = UserEntity.builder().id(1L).name("Alice").email("alice@example.com").build();
            final TimeslotEntity organizerSlot = TimeslotEntity.builder()
                .id(10L).owner(organizer).startTime(start).endTime(end).status(SlotBookingStatus.FREE).build();
            when(timeslotRepository.findAll(ArgumentMatchers.<Specification<TimeslotEntity>>any()))
                .thenReturn(List.of(organizerSlot))  // organizer has availability
                .thenReturn(List.of()); // participant has no availability
            final MeetingCreateRequestDto meetingCreateRequestDto = new MeetingCreateRequestDto(1L, "Sync", null, start, end, List.of(99L));

            // Act & Assert
            assertThatThrownBy(() -> meetingService.createMeeting(meetingCreateRequestDto))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        }

        @Test
        void throwsConflict_whenMeetingAlreadyExists() {
            // Arrange
            final LocalDateTime start = LocalDateTime.of(2026, 5, 10, 10, 0);
            final LocalDateTime end = LocalDateTime.of(2026, 5, 10, 11, 0);
            final UserEntity organizer = UserEntity.builder().id(1L).name("Alice").email("alice@example.com").build();
            final TimeslotEntity organizerSlot = TimeslotEntity.builder()
                .id(10L).owner(organizer).startTime(start).endTime(end).status(SlotBookingStatus.FREE).build();
            when(timeslotRepository.findAll(ArgumentMatchers.<Specification<TimeslotEntity>>any()))
                .thenReturn(List.of(organizerSlot));
            when(meetingRepository.existsByOrganizerIdAndStartTimeAndEndTime(1L, start, end)).thenReturn(true);
            final MeetingCreateRequestDto meetingCreateRequestDto = new MeetingCreateRequestDto(1L, "Sync", null, start, end, List.of());

            // Act & Assert
            assertThatThrownBy(() -> meetingService.createMeeting(meetingCreateRequestDto))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
        }

        @Test
        void throwsConflict_whenConcurrentInsertViolatesUniqueConstraint() {
            // Arrange
            final LocalDateTime start = LocalDateTime.of(2026, 5, 10, 10, 0);
            final LocalDateTime end = LocalDateTime.of(2026, 5, 10, 11, 0);
            final UserEntity organizer = UserEntity.builder().id(1L).name("Alice").email("alice@example.com").build();
            final TimeslotEntity organizerSlot = TimeslotEntity.builder()
                .id(10L).owner(organizer).startTime(start).endTime(end).status(SlotBookingStatus.FREE).build();
            when(timeslotRepository.findAll(ArgumentMatchers.<Specification<TimeslotEntity>>any()))
                .thenReturn(List.of(organizerSlot));
            when(meetingRepository.save(any(MeetingEntity.class)))
                .thenThrow(new DataIntegrityViolationException("unique constraint"));
            final MeetingCreateRequestDto meetingCreateRequestDto = new MeetingCreateRequestDto(1L, "Sync", null, start, end, List.of());

            // Act & Assert
            assertThatThrownBy(() -> meetingService.createMeeting(meetingCreateRequestDto))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
        }

        @Test
        void createsMeetingAndParticipants_andSplitsTimeslotExactFit() {
            // Arrange — covering slot matches meeting range exactly for both organizer and participant
            final LocalDateTime start = LocalDateTime.of(2026, 5, 10, 10, 0);
            final LocalDateTime end = LocalDateTime.of(2026, 5, 10, 11, 0);
            final UserEntity organizer = UserEntity.builder().id(1L).name("Alice").email("alice@example.com").build();
            final UserEntity participantUser = UserEntity.builder().id(2L).name("Bob").email("bob@example.com").build();
            final TimeslotEntity organizerSlot = TimeslotEntity.builder()
                .id(10L).owner(organizer).startTime(start).endTime(end).status(SlotBookingStatus.FREE).build();
            final TimeslotEntity participantSlot = TimeslotEntity.builder()
                .id(20L).owner(participantUser).startTime(start).endTime(end).status(SlotBookingStatus.FREE).build();
            final MeetingEntity savedMeetingEntity = MeetingEntity.builder()
                .id(100L).title("Sync").description("Weekly").startTime(start).endTime(end)
                .organizer(organizer).participants(List.of()).build();
            final ParticipantEntity savedParticipantEntity = ParticipantEntity.builder()
                .id(1L).meeting(savedMeetingEntity).user(participantUser).build();
            final MeetingResponseDto expected = new MeetingResponseDto(
                100L, "Sync", "Weekly", start, end,
                new UserResponseDto(1L, "Alice", "alice@example.com"),
                List.of(new ParticipantResponseDto(1L, new UserResponseDto(2L, "Bob", "bob@example.com"))));
            when(timeslotRepository.findAll(ArgumentMatchers.<Specification<TimeslotEntity>>any()))
                .thenReturn(List.of(organizerSlot))
                .thenReturn(List.of(participantSlot));
            when(meetingRepository.save(any(MeetingEntity.class))).thenReturn(savedMeetingEntity);
            when(participantRepository.saveAll(any())).thenReturn(List.of(savedParticipantEntity));
            when(meetingMapper.toDto(savedMeetingEntity)).thenReturn(expected);
            final MeetingCreateRequestDto meetingCreateRequestDto = new MeetingCreateRequestDto(1L, "Sync", "Weekly", start, end, List.of(2L));

            // Act
            final MeetingResponseDto meetingResponseDto = meetingService.createMeeting(meetingCreateRequestDto);

            // Assert
            assertThat(meetingResponseDto).isEqualTo(expected);
            assertThat(organizerSlot.getStatus()).isEqualTo(SlotBookingStatus.BOOKED);
            assertThat(participantSlot.getStatus()).isEqualTo(SlotBookingStatus.BOOKED);
        }

        @Test
        void splitsTimeslot_whenMeetingStartsAfterSlotStart() {
            // Arrange — slot 09:00-11:00, meeting 10:00-11:00 → left remainder [09:00,10:00] FREE + BOOKED [10:00,11:00]
            final LocalDateTime slotStart = LocalDateTime.of(2026, 5, 10, 9, 0);
            final LocalDateTime meetingStart = LocalDateTime.of(2026, 5, 10, 10, 0);
            final LocalDateTime end = LocalDateTime.of(2026, 5, 10, 11, 0);
            final UserEntity organizer = UserEntity.builder().id(1L).name("Alice").email("alice@example.com").build();
            final TimeslotEntity organizerSlot = TimeslotEntity.builder()
                .id(10L).owner(organizer).startTime(slotStart).endTime(end).status(SlotBookingStatus.FREE).build();
            final MeetingEntity savedMeetingEntity = MeetingEntity.builder()
                .id(100L).title("Sync").startTime(meetingStart).endTime(end)
                .organizer(organizer).participants(List.of()).build();
            final MeetingResponseDto expected = new MeetingResponseDto(
                100L, "Sync", null, meetingStart, end,
                new UserResponseDto(1L, "Alice", "alice@example.com"), List.of());
            when(timeslotRepository.findAll(ArgumentMatchers.<Specification<TimeslotEntity>>any()))
                .thenReturn(List.of(organizerSlot));
            when(timeslotRepository.save(any(TimeslotEntity.class))).thenReturn(TimeslotEntity.builder().build());
            when(meetingRepository.save(any(MeetingEntity.class))).thenReturn(savedMeetingEntity);
            when(participantRepository.saveAll(any())).thenReturn(List.of());
            when(meetingMapper.toDto(savedMeetingEntity)).thenReturn(expected);
            final MeetingCreateRequestDto meetingCreateRequestDto = new MeetingCreateRequestDto(1L, "Sync", null, meetingStart, end, List.of());

            // Act
            final MeetingResponseDto meetingResponseDto = meetingService.createMeeting(meetingCreateRequestDto);

            // Assert
            assertThat(meetingResponseDto).isEqualTo(expected);
            assertThat(organizerSlot.getEndTime()).isEqualTo(meetingStart); // existing slot shrunk to left remainder
            assertThat(organizerSlot.getStatus()).isEqualTo(SlotBookingStatus.FREE);
        }

        @Test
        void splitsTimeslot_whenMeetingEndsBeforeSlotEnd() {
            // Arrange — slot 10:00-12:00, meeting 10:00-11:00 → BOOKED [10:00,11:00] + right remainder [11:00,12:00] FREE
            final LocalDateTime start = LocalDateTime.of(2026, 5, 10, 10, 0);
            final LocalDateTime meetingEnd = LocalDateTime.of(2026, 5, 10, 11, 0);
            final LocalDateTime slotEnd = LocalDateTime.of(2026, 5, 10, 12, 0);
            final UserEntity organizer = UserEntity.builder().id(1L).name("Alice").email("alice@example.com").build();
            final TimeslotEntity organizerSlot = TimeslotEntity.builder()
                .id(10L).owner(organizer).startTime(start).endTime(slotEnd).status(SlotBookingStatus.FREE).build();
            final MeetingEntity savedMeeting = MeetingEntity.builder()
                .id(100L).title("Sync").startTime(start).endTime(meetingEnd)
                .organizer(organizer).participants(List.of()).build();
            final MeetingResponseDto expected = new MeetingResponseDto(
                100L, "Sync", null, start, meetingEnd,
                new UserResponseDto(1L, "Alice", "alice@example.com"), List.of());
            when(timeslotRepository.findAll(ArgumentMatchers.<Specification<TimeslotEntity>>any()))
                .thenReturn(List.of(organizerSlot));
            when(timeslotRepository.save(any(TimeslotEntity.class))).thenReturn(TimeslotEntity.builder().build());
            when(meetingRepository.save(any(MeetingEntity.class))).thenReturn(savedMeeting);
            when(participantRepository.saveAll(any())).thenReturn(List.of());
            when(meetingMapper.toDto(savedMeeting)).thenReturn(expected);
            final MeetingCreateRequestDto meetingCreateRequestDto = new MeetingCreateRequestDto(1L, "Sync", null, start, meetingEnd, List.of());

            // Act
            final MeetingResponseDto meetingResponseDto = meetingService.createMeeting(meetingCreateRequestDto);

            // Assert
            assertThat(meetingResponseDto).isEqualTo(expected);
            assertThat(organizerSlot.getStatus()).isEqualTo(SlotBookingStatus.BOOKED);
            assertThat(organizerSlot.getEndTime()).isEqualTo(meetingEnd);
        }
    }

    @Nested
    class deleteMeeting {

        @Test
        void throwsNotFound_whenMeetingDoesNotExist() {
            // Arrange
            when(meetingRepository.findById(99L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> meetingService.deleteMeeting(1L, 99L))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
            verify(timeslotRepository, never()).findByOwnerIdAndStartTimeAndEndTimeAndStatus(any(), any(), any(), any());
        }

        @Test
        void throwsForbidden_whenUserIsNotOrganizer() {
            // Arrange
            final LocalDateTime start = LocalDateTime.of(2026, 5, 10, 10, 0);
            final LocalDateTime end = LocalDateTime.of(2026, 5, 10, 11, 0);
            final UserEntity organizer = UserEntity.builder().id(1L).name("Alice").email("alice@example.com").build();
            final MeetingEntity meeting = MeetingEntity.builder()
                .id(100L).title("Sync").startTime(start).endTime(end)
                .organizer(organizer).participants(List.of()).build();
            when(meetingRepository.findById(100L)).thenReturn(Optional.of(meeting));

            // Act & Assert
            assertThatThrownBy(() -> meetingService.deleteMeeting(2L, 100L))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
            verify(timeslotRepository, never()).findByOwnerIdAndStartTimeAndEndTimeAndStatus(any(), any(), any(), any());
        }

        @Test
        void restoresExactFitSlot_andDelegatesMerge() {
            // Arrange — BOOKED slot matches meeting range exactly
            final LocalDateTime start = LocalDateTime.of(2026, 5, 10, 10, 0);
            final LocalDateTime end = LocalDateTime.of(2026, 5, 10, 11, 0);
            final UserEntity organizer = UserEntity.builder().id(1L).name("Alice").email("alice@example.com").build();
            final TimeslotEntity bookedSlot = TimeslotEntity.builder()
                .id(10L).owner(organizer).startTime(start).endTime(end).status(SlotBookingStatus.BOOKED).build();
            final MeetingEntity meeting = MeetingEntity.builder()
                .id(100L).title("Sync").startTime(start).endTime(end)
                .organizer(organizer).participants(List.of()).build();
            when(meetingRepository.findById(100L)).thenReturn(Optional.of(meeting));
            when(timeslotRepository.findByOwnerIdAndStartTimeAndEndTimeAndStatus(
                eq(1L), eq(start), eq(end), eq(SlotBookingStatus.BOOKED)))
                .thenReturn(Optional.of(bookedSlot));

            // Act
            meetingService.deleteMeeting(1L, 100L);

            // Assert
            assertThat(bookedSlot.getStatus()).isEqualTo(SlotBookingStatus.FREE);
            verify(timeslotService).mergeAdjacentFreeSlots(1L, start, end);
            verify(meetingRepository).delete(meeting);
        }

        @Test
        void restoresSlot_andDelegatesMergeToTimeslotService() {
            // Arrange — BOOKED [10:00,11:00] with adjacent FREE slots; merge is delegated to TimeslotService
            final LocalDateTime meetingStart = LocalDateTime.of(2026, 5, 10, 10, 0);
            final LocalDateTime meetingEnd = LocalDateTime.of(2026, 5, 10, 11, 0);
            final UserEntity organizer = UserEntity.builder().id(1L).name("Alice").email("alice@example.com").build();
            final TimeslotEntity bookedSlot = TimeslotEntity.builder()
                .id(10L).owner(organizer).startTime(meetingStart).endTime(meetingEnd).status(SlotBookingStatus.BOOKED).build();
            final MeetingEntity meeting = MeetingEntity.builder()
                .id(100L).title("Sync").startTime(meetingStart).endTime(meetingEnd)
                .organizer(organizer).participants(List.of()).build();
            when(meetingRepository.findById(100L)).thenReturn(Optional.of(meeting));
            when(timeslotRepository.findByOwnerIdAndStartTimeAndEndTimeAndStatus(
                eq(1L), eq(meetingStart), eq(meetingEnd), eq(SlotBookingStatus.BOOKED)))
                .thenReturn(Optional.of(bookedSlot));

            // Act
            meetingService.deleteMeeting(1L, 100L);

            // Assert
            assertThat(bookedSlot.getStatus()).isEqualTo(SlotBookingStatus.FREE);
            verify(timeslotService).mergeAdjacentFreeSlots(1L, meetingStart, meetingEnd);
            verify(meetingRepository).delete(meeting);
        }

        @Test
        void restoresTimeslots_forOrganizerAndParticipants() {
            // Arrange — both organizer and participant have BOOKED slots; both get restored and merge-delegated
            final LocalDateTime start = LocalDateTime.of(2026, 5, 10, 10, 0);
            final LocalDateTime end = LocalDateTime.of(2026, 5, 10, 11, 0);
            final UserEntity organizer = UserEntity.builder().id(1L).name("Alice").email("alice@example.com").build();
            final UserEntity participantUser = UserEntity.builder().id(2L).name("Bob").email("bob@example.com").build();
            final TimeslotEntity organizerSlot = TimeslotEntity.builder()
                .id(10L).owner(organizer).startTime(start).endTime(end).status(SlotBookingStatus.BOOKED).build();
            final TimeslotEntity participantSlot = TimeslotEntity.builder()
                .id(20L).owner(participantUser).startTime(start).endTime(end).status(SlotBookingStatus.BOOKED).build();
            final ParticipantEntity participant = ParticipantEntity.builder().id(1L).user(participantUser).build();
            final MeetingEntity meeting = MeetingEntity.builder()
                .id(100L).title("Sync").startTime(start).endTime(end)
                .organizer(organizer).participants(List.of(participant)).build();
            when(meetingRepository.findById(100L)).thenReturn(Optional.of(meeting));
            when(timeslotRepository.findByOwnerIdAndStartTimeAndEndTimeAndStatus(
                eq(1L), eq(start), eq(end), eq(SlotBookingStatus.BOOKED)))
                .thenReturn(Optional.of(organizerSlot));
            when(timeslotRepository.findByOwnerIdAndStartTimeAndEndTimeAndStatus(
                eq(2L), eq(start), eq(end), eq(SlotBookingStatus.BOOKED)))
                .thenReturn(Optional.of(participantSlot));

            // Act
            meetingService.deleteMeeting(1L, 100L);

            // Assert
            assertThat(organizerSlot.getStatus()).isEqualTo(SlotBookingStatus.FREE);
            assertThat(participantSlot.getStatus()).isEqualTo(SlotBookingStatus.FREE);
            verify(timeslotService).mergeAdjacentFreeSlots(1L, start, end);
            verify(timeslotService).mergeAdjacentFreeSlots(2L, start, end);
            verify(meetingRepository).delete(meeting);
        }
    }
}
