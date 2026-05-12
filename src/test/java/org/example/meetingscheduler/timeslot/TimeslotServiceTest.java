package org.example.meetingscheduler.timeslot;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.example.meetingscheduler.timeslot.dto.TimeslotResponseDto;
import org.example.meetingscheduler.timeslot.dto.TimeslotUpdateRequestDto;
import org.example.meetingscheduler.user.UserEntity;
import org.example.meetingscheduler.user.UserRepository;
import org.example.meetingscheduler.user.dto.UserResponseDto;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TimeslotServiceTest {

    @InjectMocks
    private TimeslotService timeslotService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private TimeslotRepository timeslotRepository;
    @Mock
    private TimeslotMapper timeslotMapper;

    @Nested
    class createTimeslot {

        @Test
        void savesAndReturnsMappedDto() {
            // Arrange
            final LocalDateTime start = LocalDateTime.of(2026, 5, 10, 10, 0);
            final LocalDateTime end = LocalDateTime.of(2026, 5, 10, 11, 0);
            final UserEntity userEntity = UserEntity.builder()
                .id(1L).name("Alice").email("alice@example.com").build();
            final TimeslotEntity unsaved = TimeslotEntity.builder()
                .owner(userEntity).startTime(start).endTime(end).status(SlotBookingStatus.FREE).build();
            final TimeslotEntity saved = TimeslotEntity.builder()
                .id(1L).owner(userEntity).startTime(start).endTime(end).status(SlotBookingStatus.FREE).build();
            final TimeslotResponseDto timeslotResponseDto = new TimeslotResponseDto(
                1L, new UserResponseDto(1L, "Alice", "alice@example.com"), start, end, SlotBookingStatus.FREE);
            when(userRepository.findByIdIs(1L)).thenReturn(Optional.of(userEntity));
            when(timeslotRepository.save(unsaved)).thenReturn(saved);
            when(timeslotMapper.toDto(saved)).thenReturn(timeslotResponseDto);

            // Act & Assert
            assertThat(timeslotService.createTimeslot(1L, start, end)).isEqualTo(timeslotResponseDto);
        }

        @Test
        void throwsConflict_whenDuplicateExists() {
            // Arrange
            final LocalDateTime start = LocalDateTime.of(2026, 5, 10, 10, 0);
            final LocalDateTime end = LocalDateTime.of(2026, 5, 10, 11, 0);
            final UserEntity organizer = UserEntity.builder().id(1L).name("Alice").email("alice@example.com").build();
            final TimeslotEntity existing = TimeslotEntity.builder()
                .id(1L).owner(organizer).startTime(start).endTime(end).status(SlotBookingStatus.FREE).build();
            when(userRepository.findByIdIs(1L)).thenReturn(Optional.of(organizer));
            when(timeslotRepository.findAll(ArgumentMatchers.<Specification<TimeslotEntity>>any()))
                .thenReturn(List.of(existing)); // coversRange matches exact duplicate

            // Act & Assert
            assertThatThrownBy(() -> timeslotService.createTimeslot(1L, start, end))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
        }

        @Test
        void throwsConflict_whenNewSlotIsContainedWithinExistingSlot() {
            // Arrange: existing 09:00-12:00, new 10:00-11:00 — fully contained, not a partial overlap
            final LocalDateTime newStart = LocalDateTime.of(2026, 5, 10, 10, 0);
            final LocalDateTime newEnd = LocalDateTime.of(2026, 5, 10, 11, 0);
            final UserEntity organizer = UserEntity.builder().id(1L).name("Alice").email("alice@example.com").build();
            final TimeslotEntity coveringTimeslot = TimeslotEntity.builder()
                .id(1L).owner(organizer)
                .startTime(LocalDateTime.of(2026, 5, 10, 9, 0))
                .endTime(LocalDateTime.of(2026, 5, 10, 12, 0))
                .status(SlotBookingStatus.FREE).build();
            when(userRepository.findByIdIs(1L)).thenReturn(Optional.of(organizer));
            when(timeslotRepository.findAll(ArgumentMatchers.<Specification<TimeslotEntity>>any()))
                .thenReturn(List.of(coveringTimeslot));

            // Act & Assert
            assertThatThrownBy(() -> timeslotService.createTimeslot(1L, newStart, newEnd))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
        }

        @Test
        void mergesWithAdjacentTimeslot() {
            // Arrange: existing 10:00-11:00, new 11:00-12:00 → merged 10:00-12:00
            final LocalDateTime existingStart = LocalDateTime.of(2026, 5, 10, 10, 0);
            final LocalDateTime existingEnd = LocalDateTime.of(2026, 5, 10, 11, 0);
            final LocalDateTime newStart = LocalDateTime.of(2026, 5, 10, 11, 0);
            final LocalDateTime newEnd = LocalDateTime.of(2026, 5, 10, 12, 0);
            final UserEntity organizer = UserEntity.builder().id(1L).name("Alice").email("alice@example.com").build();
            final TimeslotEntity existingTimeslot = TimeslotEntity.builder()
                .id(1L).owner(organizer).startTime(existingStart).endTime(existingEnd).status(SlotBookingStatus.FREE).build();
            final TimeslotResponseDto expected = new TimeslotResponseDto(
                1L, new UserResponseDto(1L, "Alice", "alice@example.com"), existingStart, newEnd, SlotBookingStatus.FREE);
            when(userRepository.findByIdIs(1L)).thenReturn(Optional.of(organizer));
            when(timeslotRepository.findAll(ArgumentMatchers.<Specification<TimeslotEntity>>any()))
                .thenReturn(List.of())          // coversRange check: no covering slot
                .thenReturn(List.of(existingTimeslot)); // overlapsOrAdjacent: merge candidate
            when(timeslotMapper.toDto(existingTimeslot)).thenReturn(expected);

            // Act
            final TimeslotResponseDto result = timeslotService.createTimeslot(1L, newStart, newEnd);

            // Assert
            assertThat(result).isEqualTo(expected);
            assertThat(existingTimeslot.getStartTime()).isEqualTo(existingStart);
            assertThat(existingTimeslot.getEndTime()).isEqualTo(newEnd);
        }

        @Test
        void mergesWithOverlappingTimeslot() {
            // Arrange: existing 10:00-11:00, new 10:30-11:30 → merged 10:00-11:30
            final LocalDateTime existingStart = LocalDateTime.of(2026, 5, 10, 10, 0);
            final LocalDateTime existingEnd = LocalDateTime.of(2026, 5, 10, 11, 0);
            final LocalDateTime newStart = LocalDateTime.of(2026, 5, 10, 10, 30);
            final LocalDateTime newEnd = LocalDateTime.of(2026, 5, 10, 11, 30);
            final UserEntity organizer = UserEntity.builder().id(1L).name("Alice").email("alice@example.com").build();
            final TimeslotEntity existing = TimeslotEntity.builder()
                .id(1L).owner(organizer).startTime(existingStart).endTime(existingEnd).status(SlotBookingStatus.FREE).build();
            final TimeslotResponseDto expected = new TimeslotResponseDto(
                1L, new UserResponseDto(1L, "Alice", "alice@example.com"), existingStart, newEnd, SlotBookingStatus.FREE);
            when(userRepository.findByIdIs(1L)).thenReturn(Optional.of(organizer));
            when(timeslotRepository.findAll(ArgumentMatchers.<Specification<TimeslotEntity>>any()))
                .thenReturn(List.of())          // coversRange check: no covering slot
                .thenReturn(List.of(existing)); // overlapsOrAdjacent: merge candidate
            when(timeslotMapper.toDto(existing)).thenReturn(expected);

            // Act
            final TimeslotResponseDto result = timeslotService.createTimeslot(1L, newStart, newEnd);

            // Assert
            assertThat(result).isEqualTo(expected);
            assertThat(existing.getStartTime()).isEqualTo(existingStart);
            assertThat(existing.getEndTime()).isEqualTo(newEnd);
        }

        @Test
        void mergesWithCoveringTimeslot() {
            // Arrange: existing 10:00-11:00, new 09:00-13:00 → merged 09:00-13:00
            final LocalDateTime existingStart = LocalDateTime.of(2026, 5, 10, 10, 0);
            final LocalDateTime existingEnd = LocalDateTime.of(2026, 5, 10, 11, 0);
            final LocalDateTime newStart = LocalDateTime.of(2026, 5, 10, 9, 0);
            final LocalDateTime newEnd = LocalDateTime.of(2026, 5, 10, 13, 0);
            final UserEntity organizer = UserEntity.builder().id(1L).name("Alice").email("alice@example.com").build();
            final TimeslotEntity existing = TimeslotEntity.builder()
                .id(1L).owner(organizer).startTime(existingStart).endTime(existingEnd).status(SlotBookingStatus.FREE).build();
            final TimeslotResponseDto expected = new TimeslotResponseDto(
                1L, new UserResponseDto(1L, "Alice", "alice@example.com"), newStart, newEnd, SlotBookingStatus.FREE);
            when(userRepository.findByIdIs(1L)).thenReturn(Optional.of(organizer));
            when(timeslotRepository.findAll(ArgumentMatchers.<Specification<TimeslotEntity>>any()))
                .thenReturn(List.of())          // coversRange check: no covering slot
                .thenReturn(List.of(existing)); // overlapsOrAdjacent: merge candidate
            when(timeslotMapper.toDto(existing)).thenReturn(expected);

            // Act
            final TimeslotResponseDto result = timeslotService.createTimeslot(1L, newStart, newEnd);

            // Assert
            assertThat(result).isEqualTo(expected);
            assertThat(existing.getStartTime()).isEqualTo(newStart);
            assertThat(existing.getEndTime()).isEqualTo(newEnd);
        }

        @Test
        void throwsConflict_whenNewSlotOverlapsWithBookedSlot() {
            // Arrange: existing [10:00-11:00 BOOKED], new [09:00-12:00] wraps it
            final LocalDateTime newStart = LocalDateTime.of(2026, 5, 10, 9, 0);
            final LocalDateTime newEnd = LocalDateTime.of(2026, 5, 10, 12, 0);
            final UserEntity organizer = UserEntity.builder().id(1L).name("Alice").email("alice@example.com").build();
            final TimeslotEntity bookedSlot = TimeslotEntity.builder()
                .id(1L).owner(organizer)
                .startTime(LocalDateTime.of(2026, 5, 10, 10, 0))
                .endTime(LocalDateTime.of(2026, 5, 10, 11, 0))
                .status(SlotBookingStatus.BOOKED).build();
            when(userRepository.findByIdIs(1L)).thenReturn(Optional.of(organizer));
            when(timeslotRepository.findAll(ArgumentMatchers.<Specification<TimeslotEntity>>any()))
                .thenReturn(List.of()) // coversRange: BOOKED slot does not cover new range
                .thenReturn(List.of(bookedSlot)); // overlapsOrAdjacent: BOOKED slot overlaps

            // Act & Assert
            assertThatThrownBy(() -> timeslotService.createTimeslot(1L, newStart, newEnd))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
        }

        @Test
        void throwsBadRequest_whenEndTimeEqualsStartTime() {
            // Arrange
            final LocalDateTime time = LocalDateTime.of(2026, 5, 10, 10, 0);

            // Act & Assert
            assertThatThrownBy(() -> timeslotService.createTimeslot(1L, time, time))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        void throwsBadRequest_whenEndTimeBeforeStartTime() {
            // Arrange
            final LocalDateTime start = LocalDateTime.of(2026, 5, 10, 11, 0);
            final LocalDateTime end = LocalDateTime.of(2026, 5, 10, 10, 0);

            // Act & Assert
            assertThatThrownBy(() -> timeslotService.createTimeslot(1L, start, end))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        void throwsBadRequest_whenStartAndEndTimeSpanMidnight() {
            // Arrange: 23:30 on one day to 00:30 on the next
            final LocalDateTime start = LocalDateTime.of(2026, 5, 10, 23, 30);
            final LocalDateTime end = LocalDateTime.of(2026, 5, 11, 0, 30);

            // Act & Assert
            assertThatThrownBy(() -> timeslotService.createTimeslot(1L, start, end))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    @Nested
    class getTimeslots {

        @Test
        void returnsAllTimeslots_whenNoFilters() {
            // Arrange
            final LocalDateTime start = LocalDateTime.of(2026, 5, 10, 10, 0);
            final LocalDateTime end = LocalDateTime.of(2026, 5, 10, 11, 0);
            final UserEntity userEntity = UserEntity.builder()
                .id(1L).name("Alice").email("alice@example.com").build();
            final TimeslotEntity timeslotEntity = TimeslotEntity.builder()
                .id(1L).owner(userEntity).startTime(start).endTime(end).status(SlotBookingStatus.FREE).build();
            final TimeslotResponseDto timeslotResponseDto = new TimeslotResponseDto(
                1L, new UserResponseDto(1L, "Alice", "alice@example.com"), start, end, SlotBookingStatus.FREE);
            when(timeslotRepository.findAll(ArgumentMatchers.<Specification<TimeslotEntity>>any(), any(Sort.class))).thenReturn(List.of(timeslotEntity));
            when(timeslotMapper.toDto(timeslotEntity)).thenReturn(timeslotResponseDto);

            // Act & Assert
            assertThat(timeslotService.getTimeslots(1L, null, null, null))
                .containsExactly(timeslotResponseDto);
        }

        @Test
        void returnsEmptyList_whenNoTimeslots() {
            // Arrange
            when(timeslotRepository.findAll(ArgumentMatchers.<Specification<TimeslotEntity>>any(), any(Sort.class))).thenReturn(List.of());

            // Act & Assert
            assertThat(timeslotService.getTimeslots(1L, null, null, null)).isEmpty();
        }

        @Test
        void returnsFilteredList_whenStatusProvided() {
            // Arrange
            final LocalDateTime start = LocalDateTime.of(2026, 5, 10, 10, 0);
            final LocalDateTime end = LocalDateTime.of(2026, 5, 10, 11, 0);
            final UserEntity userEntity = UserEntity.builder()
                .id(1L).name("Alice").email("alice@example.com").build();
            final TimeslotEntity timeslotEntity = TimeslotEntity.builder()
                .id(2L).owner(userEntity).startTime(start).endTime(end).status(SlotBookingStatus.BOOKED).build();
            final TimeslotResponseDto timeslotResponseDto = new TimeslotResponseDto(
                2L, new UserResponseDto(1L, "Alice", "alice@example.com"), start, end, SlotBookingStatus.BOOKED);
            when(timeslotRepository.findAll(ArgumentMatchers.<Specification<TimeslotEntity>>any(), any(Sort.class))).thenReturn(List.of(timeslotEntity));
            when(timeslotMapper.toDto(timeslotEntity)).thenReturn(timeslotResponseDto);

            // Act & Assert
            assertThat(timeslotService.getTimeslots(1L, null, null, SlotBookingStatus.BOOKED))
                .containsExactly(timeslotResponseDto);
        }
    }

    @Nested
    class updateTimeslot {

        @Test
        void updatesAllFields_andReturnsDto() {
            // Arrange
            final LocalDateTime newStart = LocalDateTime.of(2026, 5, 10, 11, 0);
            final LocalDateTime newEnd = LocalDateTime.of(2026, 5, 10, 12, 0);
            final UserEntity userEntity = UserEntity.builder()
                .id(1L).name("Alice").email("alice@example.com").build();
            final TimeslotEntity existing = TimeslotEntity.builder()
                .id(1L)
                .owner(userEntity)
                .startTime(LocalDateTime.of(2026, 5, 10, 9, 0))
                .endTime(LocalDateTime.of(2026, 5, 10, 10, 0))
                .status(SlotBookingStatus.FREE).build();
            final TimeslotUpdateRequestDto request = new TimeslotUpdateRequestDto(newStart, newEnd, SlotBookingStatus.BOOKED);
            final TimeslotResponseDto dto = new TimeslotResponseDto(
                1L, new UserResponseDto(1L, "Alice", "alice@example.com"), newStart, newEnd, SlotBookingStatus.BOOKED);
            when(timeslotRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(timeslotMapper.toDto(existing)).thenReturn(dto);

            // Act & Assert
            assertThat(timeslotService.updateTimeslot(1L, 1L, request)).isEqualTo(dto);
        }

        @Test
        void updatesOnlyStatus_preservingExistingTimes() {
            // Arrange
            final LocalDateTime start = LocalDateTime.of(2026, 5, 10, 10, 0);
            final LocalDateTime end = LocalDateTime.of(2026, 5, 10, 11, 0);
            final UserEntity userEntity = UserEntity.builder()
                .id(1L).name("Alice").email("alice@example.com").build();
            final TimeslotEntity existing = TimeslotEntity.builder()
                .id(1L).owner(userEntity).startTime(start).endTime(end).status(SlotBookingStatus.FREE).build();
            final TimeslotUpdateRequestDto timeslotUpdateRequestDto = new TimeslotUpdateRequestDto(null, null, SlotBookingStatus.BOOKED);
            final TimeslotResponseDto timeslotResponseDto = new TimeslotResponseDto(
                1L, new UserResponseDto(1L, "Alice", "alice@example.com"), start, end, SlotBookingStatus.BOOKED);
            when(timeslotRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(timeslotMapper.toDto(existing)).thenReturn(timeslotResponseDto);

            // Act & Assert
            assertThat(timeslotService.updateTimeslot(1L, 1L, timeslotUpdateRequestDto)).isEqualTo(timeslotResponseDto);
        }

        @Test
        void throwsNotFound_whenTimeslotDoesNotExist() {
            // Arrange
            when(timeslotRepository.findById(99L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> timeslotService.updateTimeslot(
                    1L, 99L, new TimeslotUpdateRequestDto(null, null, SlotBookingStatus.BOOKED)))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        void throwsForbidden_whenTimeslotBelongsToAnotherUser() {
            // Arrange
            final UserEntity owner = UserEntity.builder().id(1L).name("Alice").email("alice@example.com").build();
            final TimeslotEntity timeslot = TimeslotEntity.builder()
                .id(1L).owner(owner).status(SlotBookingStatus.FREE).build();
            when(timeslotRepository.findById(1L)).thenReturn(Optional.of(timeslot));

            // Act & Assert
            assertThatThrownBy(() -> timeslotService.updateTimeslot(
                    2L, 1L, new TimeslotUpdateRequestDto(null, null, SlotBookingStatus.BOOKED)))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        }

        @Test
        void throwsConflict_whenTimeslotIsBooked() {
            // Arrange
            final UserEntity userEntity = UserEntity.builder().id(1L).name("Alice").email("alice@example.com").build();
            final TimeslotEntity bookedTimeslot = TimeslotEntity.builder()
                .id(1L).owner(userEntity).status(SlotBookingStatus.BOOKED).build();
            when(timeslotRepository.findById(1L)).thenReturn(Optional.of(bookedTimeslot));

            // Act & Assert
            assertThatThrownBy(() -> timeslotService.updateTimeslot(
                    1L, 1L, new TimeslotUpdateRequestDto(null, null, SlotBookingStatus.FREE)))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
        }

        @Test
        void throwsBadRequest_whenEffectiveTimeRangeIsInvalid() {
            // Arrange
            final UserEntity userEntity = UserEntity.builder()
                .id(1L).name("Alice").email("alice@example.com").build();
            final TimeslotEntity existing = TimeslotEntity.builder()
                .id(1L)
                .owner(userEntity)
                .startTime(LocalDateTime.of(2026, 5, 10, 10, 0))
                .endTime(LocalDateTime.of(2026, 5, 10, 11, 0))
                .status(SlotBookingStatus.FREE).build();
            final TimeslotUpdateRequestDto timeslotUpdateRequestDto = new TimeslotUpdateRequestDto(
                null, LocalDateTime.of(2026, 5, 10, 9, 0), null); // new endTime is before existing startTime
            when(timeslotRepository.findById(1L)).thenReturn(Optional.of(existing));

            // Act & Assert
            assertThatThrownBy(() -> timeslotService.updateTimeslot(1L, 1L, timeslotUpdateRequestDto))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    @Nested
    class deleteTimeslot {

        @Test
        void deletesTimeslot() {
            // Arrange
            final UserEntity owner = UserEntity.builder().id(1L).name("Alice").email("alice@example.com").build();
            final TimeslotEntity free = TimeslotEntity.builder()
                .id(1L).owner(owner)
                .startTime(LocalDateTime.of(2026, 5, 10, 10, 0))
                .endTime(LocalDateTime.of(2026, 5, 10, 11, 0))
                .status(SlotBookingStatus.FREE).build();
            when(timeslotRepository.findById(1L)).thenReturn(Optional.of(free));

            // Act
            timeslotService.deleteTimeslot(1L, 1L);

            // Assert
            verify(timeslotRepository).deleteById(1L);
        }

        @Test
        void throwsNotFound_whenTimeslotDoesNotExist() {
            // Arrange
            when(timeslotRepository.findById(99L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> timeslotService.deleteTimeslot(1L, 99L))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        void throwsForbidden_whenTimeslotBelongsToAnotherUser() {
            // Arrange
            final UserEntity owner = UserEntity.builder().id(1L).name("Alice").email("alice@example.com").build();
            final TimeslotEntity free = TimeslotEntity.builder()
                .id(1L).owner(owner)
                .startTime(LocalDateTime.of(2026, 5, 10, 10, 0))
                .endTime(LocalDateTime.of(2026, 5, 10, 11, 0))
                .status(SlotBookingStatus.FREE).build();
            when(timeslotRepository.findById(1L)).thenReturn(Optional.of(free));

            // Act & Assert
            assertThatThrownBy(() -> timeslotService.deleteTimeslot(2L, 1L))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
            verify(timeslotRepository, never()).deleteById(any());
        }

        @Test
        void throwsConflict_whenTimeslotIsBooked() {
            // Arrange
            final UserEntity owner = UserEntity.builder().id(1L).name("Alice").email("alice@example.com").build();
            final TimeslotEntity booked = TimeslotEntity.builder()
                .id(10L).owner(owner)
                .startTime(LocalDateTime.of(2026, 5, 10, 10, 0))
                .endTime(LocalDateTime.of(2026, 5, 10, 11, 0))
                .status(SlotBookingStatus.BOOKED).build();
            when(timeslotRepository.findById(10L)).thenReturn(Optional.of(booked));

            // Act & Assert
            assertThatThrownBy(() -> timeslotService.deleteTimeslot(1L, 10L))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
            verify(timeslotRepository, never()).deleteById(any());
        }
    }

    @Nested
    class mergeAdjacentFreeSlots {

        @Test
        void doesNothing_whenOnlyOneAdjacentSlot() {
            // Arrange — only the restored BOOKED slot itself is returned; nothing to merge
            final LocalDateTime start = LocalDateTime.of(2026, 5, 10, 10, 0);
            final LocalDateTime end = LocalDateTime.of(2026, 5, 10, 11, 0);
            final UserEntity owner = UserEntity.builder().id(1L).name("Alice").email("alice@example.com").build();
            final TimeslotEntity onlySlot = TimeslotEntity.builder()
                .id(10L).owner(owner).startTime(start).endTime(end).status(SlotBookingStatus.FREE).build();
            when(timeslotRepository.findAll(ArgumentMatchers.<Specification<TimeslotEntity>>any()))
                .thenReturn(List.of(onlySlot));

            // Act
            timeslotService.mergeAdjacentFreeSlots(1L, start, end);

            // Assert — no deleteAll call; the single slot is left unchanged
            verify(timeslotRepository, never()).deleteAll(any());
        }

        @Test
        void mergesAllAdjacentSlots_intoFirstSlot() {
            // Arrange — three adjacent FREE slots: [09:00,10:00] + [10:00,11:00] + [11:00,12:00]
            final LocalDateTime around = LocalDateTime.of(2026, 5, 10, 10, 0);
            final LocalDateTime until = LocalDateTime.of(2026, 5, 10, 11, 0);
            final UserEntity owner = UserEntity.builder().id(1L).name("Alice").email("alice@example.com").build();
            final TimeslotEntity left = TimeslotEntity.builder()
                .id(11L).owner(owner).startTime(LocalDateTime.of(2026, 5, 10, 9, 0)).endTime(around).status(SlotBookingStatus.FREE).build();
            final TimeslotEntity middle = TimeslotEntity.builder()
                .id(10L).owner(owner).startTime(around).endTime(until).status(SlotBookingStatus.FREE).build();
            final TimeslotEntity right = TimeslotEntity.builder()
                .id(12L).owner(owner).startTime(until).endTime(LocalDateTime.of(2026, 5, 10, 12, 0)).status(SlotBookingStatus.FREE).build();
            when(timeslotRepository.findAll(ArgumentMatchers.<Specification<TimeslotEntity>>any()))
                .thenReturn(List.of(left, middle, right));

            // Act
            timeslotService.mergeAdjacentFreeSlots(1L, around, until);

            // Assert — first slot expanded to full range; the other two deleted
            assertThat(left.getStartTime()).isEqualTo(LocalDateTime.of(2026, 5, 10, 9, 0));
            assertThat(left.getEndTime()).isEqualTo(LocalDateTime.of(2026, 5, 10, 12, 0));
            verify(timeslotRepository).deleteAll(List.of(middle, right));
        }
    }
}
