package org.example.meetingscheduler.timeslot;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.example.meetingscheduler.timeslot.dto.TimeslotResponseDto;
import org.example.meetingscheduler.timeslot.dto.TimeslotUpdateRequestDto;
import org.example.meetingscheduler.user.UserEntity;
import org.example.meetingscheduler.user.UserRepository;
import org.example.meetingscheduler.user.dto.UserResponseDto;
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

    @Test
    void createTimeslot_savesAndReturnsMappedDto() {
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
        when(this.userRepository.findByIdIs(1L)).thenReturn(Optional.of(userEntity));
        when(this.timeslotRepository.save(unsaved)).thenReturn(saved);
        when(this.timeslotMapper.toDto(saved)).thenReturn(timeslotResponseDto);

        // Act & Assert
        assertThat(this.timeslotService.createTimeslot(1L, start, end)).isEqualTo(timeslotResponseDto);
    }

    @Test
    void createTimeslot_throwsConflict_whenDuplicateExists() {
        // Arrange
        final LocalDateTime start = LocalDateTime.of(2026, 5, 10, 10, 0);
        final LocalDateTime end = LocalDateTime.of(2026, 5, 10, 11, 0);
        final UserEntity organizer = UserEntity.builder().id(1L).name("Alice").email("alice@example.com").build();
        final TimeslotEntity existing = TimeslotEntity.builder()
            .id(1L).startTime(start).endTime(end).status(SlotBookingStatus.FREE).build();
        when(this.userRepository.findByIdIs(1L)).thenReturn(Optional.of(organizer));
        when(this.timeslotRepository.findByOwnerIdAndStartTimeAndEndTime(1L, start, end)).thenReturn(Optional.of(existing));

        // Act & Assert
        assertThatThrownBy(() -> this.timeslotService.createTimeslot(1L, start, end))
            .isInstanceOf(ResponseStatusException.class)
            .extracting(e -> ((ResponseStatusException) e).getStatusCode())
            .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void createTimeslot_mergesWithAdjacentTimeslot() {
        // Arrange: existing 10:00-11:00, new 11:00-12:00 → merged 10:00-12:00
        final LocalDateTime existingStart = LocalDateTime.of(2026, 5, 10, 10, 0);
        final LocalDateTime existingEnd = LocalDateTime.of(2026, 5, 10, 11, 0);
        final LocalDateTime newStart = LocalDateTime.of(2026, 5, 10, 11, 0);
        final LocalDateTime newEnd = LocalDateTime.of(2026, 5, 10, 12, 0);
        final UserEntity organizer = UserEntity.builder().id(1L).name("Alice").email("alice@example.com").build();
        final TimeslotEntity existing = TimeslotEntity.builder()
            .id(1L).owner(organizer).startTime(existingStart).endTime(existingEnd).status(SlotBookingStatus.FREE).build();
        final TimeslotResponseDto expected = new TimeslotResponseDto(
            1L, new UserResponseDto(1L, "Alice", "alice@example.com"), existingStart, newEnd, SlotBookingStatus.FREE);
        when(this.userRepository.findByIdIs(1L)).thenReturn(Optional.of(organizer));
        when(this.timeslotRepository.findAll(ArgumentMatchers.<Specification<TimeslotEntity>>any()))
            .thenReturn(List.of(existing));
        when(this.timeslotMapper.toDto(existing)).thenReturn(expected);

        // Act
        final TimeslotResponseDto result = this.timeslotService.createTimeslot(1L, newStart, newEnd);

        // Assert
        assertThat(result).isEqualTo(expected);
        assertThat(existing.getStartTime()).isEqualTo(existingStart);
        assertThat(existing.getEndTime()).isEqualTo(newEnd);
    }

    @Test
    void createTimeslot_mergesWithOverlappingTimeslot() {
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
        when(this.userRepository.findByIdIs(1L)).thenReturn(Optional.of(organizer));
        when(this.timeslotRepository.findAll(ArgumentMatchers.<Specification<TimeslotEntity>>any()))
            .thenReturn(List.of(existing));
        when(this.timeslotMapper.toDto(existing)).thenReturn(expected);

        // Act
        final TimeslotResponseDto result = this.timeslotService.createTimeslot(1L, newStart, newEnd);

        // Assert
        assertThat(result).isEqualTo(expected);
        assertThat(existing.getStartTime()).isEqualTo(existingStart);
        assertThat(existing.getEndTime()).isEqualTo(newEnd);
    }

    @Test
    void createTimeslot_mergesWithCoveringTimeslot() {
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
        when(this.userRepository.findByIdIs(1L)).thenReturn(Optional.of(organizer));
        when(this.timeslotRepository.findAll(ArgumentMatchers.<Specification<TimeslotEntity>>any()))
            .thenReturn(List.of(existing));
        when(this.timeslotMapper.toDto(existing)).thenReturn(expected);

        // Act
        final TimeslotResponseDto result = this.timeslotService.createTimeslot(1L, newStart, newEnd);

        // Assert
        assertThat(result).isEqualTo(expected);
        assertThat(existing.getStartTime()).isEqualTo(newStart);
        assertThat(existing.getEndTime()).isEqualTo(newEnd);
    }

    @Test
    void createTimeslot_throwsBadRequest_whenEndTimeEqualsStartTime() {
        // Arrange
        final LocalDateTime time = LocalDateTime.of(2026, 5, 10, 10, 0);

        // Act & Assert
        assertThatThrownBy(() -> this.timeslotService.createTimeslot(1L, time, time))
            .isInstanceOf(ResponseStatusException.class)
            .extracting(e -> ((ResponseStatusException) e).getStatusCode())
            .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void createTimeslot_throwsBadRequest_whenEndTimeBeforeStartTime() {
        // Arrange
        final LocalDateTime start = LocalDateTime.of(2026, 5, 10, 11, 0);
        final LocalDateTime end = LocalDateTime.of(2026, 5, 10, 10, 0);

        // Act & Assert
        assertThatThrownBy(() -> this.timeslotService.createTimeslot(1L, start, end))
            .isInstanceOf(ResponseStatusException.class)
            .extracting(e -> ((ResponseStatusException) e).getStatusCode())
            .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void getTimeslots_returnsAllTimeslots_whenNoFilters() {
        // Arrange
        final LocalDateTime start = LocalDateTime.of(2026, 5, 10, 10, 0);
        final LocalDateTime end = LocalDateTime.of(2026, 5, 10, 11, 0);
        final UserEntity userEntity = UserEntity.builder()
            .id(1L).name("Alice").email("alice@example.com").build();
        final TimeslotEntity timeslotEntity = TimeslotEntity.builder()
            .id(1L).owner(userEntity).startTime(start).endTime(end).status(SlotBookingStatus.FREE).build();
        final TimeslotResponseDto timeslotResponseDto = new TimeslotResponseDto(
            1L, new UserResponseDto(1L, "Alice", "alice@example.com"), start, end, SlotBookingStatus.FREE);
        when(this.timeslotRepository.findAll(ArgumentMatchers.<Specification<TimeslotEntity>>any(), any(Sort.class))).thenReturn(List.of(timeslotEntity));
        when(this.timeslotMapper.toDto(timeslotEntity)).thenReturn(timeslotResponseDto);

        // Act & Assert
        assertThat(this.timeslotService.getTimeslots(1L, null, null, null))
            .containsExactly(timeslotResponseDto);
    }

    @Test
    void getTimeslots_returnsEmptyList_whenNoTimeslots() {
        // Arrange
        when(this.timeslotRepository.findAll(ArgumentMatchers.<Specification<TimeslotEntity>>any(), any(Sort.class))).thenReturn(List.of());

        // Act & Assert
        assertThat(this.timeslotService.getTimeslots(1L, null, null, null)).isEmpty();
    }

    @Test
    void getTimeslots_returnsFilteredList_whenStatusProvided() {
        // Arrange
        final LocalDateTime start = LocalDateTime.of(2026, 5, 10, 10, 0);
        final LocalDateTime end = LocalDateTime.of(2026, 5, 10, 11, 0);
        final UserEntity userEntity = UserEntity.builder()
            .id(1L).name("Alice").email("alice@example.com").build();
        final TimeslotEntity timeslotEntity = TimeslotEntity.builder()
            .id(2L).owner(userEntity).startTime(start).endTime(end).status(SlotBookingStatus.BOOKED).build();
        final TimeslotResponseDto timeslotResponseDto = new TimeslotResponseDto(
            2L, new UserResponseDto(1L, "Alice", "alice@example.com"), start, end, SlotBookingStatus.BOOKED);
        when(this.timeslotRepository.findAll(ArgumentMatchers.<Specification<TimeslotEntity>>any(), any(Sort.class))).thenReturn(List.of(timeslotEntity));
        when(this.timeslotMapper.toDto(timeslotEntity)).thenReturn(timeslotResponseDto);

        // Act & Assert
        assertThat(this.timeslotService.getTimeslots(1L, null, null, SlotBookingStatus.BOOKED))
            .containsExactly(timeslotResponseDto);
    }

    @Test
    void updateTimeslot_updatesAllFields_andReturnsDto() {
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
        when(this.timeslotRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(this.timeslotMapper.toDto(existing)).thenReturn(dto);

        // Act & Assert
        assertThat(this.timeslotService.updateTimeslot(1L, request)).isEqualTo(dto);
    }

    @Test
    void updateTimeslot_updatesOnlyStatus_preservingExistingTimes() {
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
        when(this.timeslotRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(this.timeslotMapper.toDto(existing)).thenReturn(timeslotResponseDto);

        // Act & Assert
        assertThat(this.timeslotService.updateTimeslot(1L, timeslotUpdateRequestDto)).isEqualTo(timeslotResponseDto);
    }

    @Test
    void updateTimeslot_throwsNotFound_whenTimeslotDoesNotExist() {
        // Arrange
        when(this.timeslotRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> this.timeslotService.updateTimeslot(
                99L, new TimeslotUpdateRequestDto(null, null, SlotBookingStatus.BOOKED)))
            .isInstanceOf(ResponseStatusException.class)
            .extracting(e -> ((ResponseStatusException) e).getStatusCode())
            .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void updateTimeslot_throwsBadRequest_whenEffectiveTimeRangeIsInvalid() {
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
        when(this.timeslotRepository.findById(1L)).thenReturn(Optional.of(existing));

        // Act & Assert
        assertThatThrownBy(() -> this.timeslotService.updateTimeslot(1L, timeslotUpdateRequestDto))
            .isInstanceOf(ResponseStatusException.class)
            .extracting(e -> ((ResponseStatusException) e).getStatusCode())
            .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void deleteTimeslot_deletesTimeslot() {
        // Arrange
        final UserEntity owner = UserEntity.builder().id(1L).name("Alice").email("alice@example.com").build();
        final TimeslotEntity free = TimeslotEntity.builder()
            .id(1L).owner(owner)
            .startTime(LocalDateTime.of(2026, 5, 10, 10, 0))
            .endTime(LocalDateTime.of(2026, 5, 10, 11, 0))
            .status(SlotBookingStatus.FREE).build();
        when(this.timeslotRepository.findById(1L)).thenReturn(Optional.of(free));

        // Act
        this.timeslotService.deleteTimeslot(1L);

        // Assert
        verify(this.timeslotRepository).deleteById(1L);
    }

    @Test
    void deleteTimeslot_throwsNotFound_whenTimeslotDoesNotExist() {
        // Arrange
        when(this.timeslotRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> this.timeslotService.deleteTimeslot(99L))
            .isInstanceOf(ResponseStatusException.class)
            .extracting(e -> ((ResponseStatusException) e).getStatusCode())
            .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void deleteTimeslot_throwsConflict_whenTimeslotIsBooked() {
        // Arrange
        final UserEntity owner = UserEntity.builder().id(1L).name("Alice").email("alice@example.com").build();
        final TimeslotEntity booked = TimeslotEntity.builder()
            .id(10L).owner(owner)
            .startTime(LocalDateTime.of(2026, 5, 10, 10, 0))
            .endTime(LocalDateTime.of(2026, 5, 10, 11, 0))
            .status(SlotBookingStatus.BOOKED).build();
        when(this.timeslotRepository.findById(10L)).thenReturn(Optional.of(booked));

        // Act & Assert
        assertThatThrownBy(() -> this.timeslotService.deleteTimeslot(10L))
            .isInstanceOf(ResponseStatusException.class)
            .extracting(e -> ((ResponseStatusException) e).getStatusCode())
            .isEqualTo(HttpStatus.CONFLICT);
        verify(this.timeslotRepository, never()).deleteById(any());
    }

    @Test
    void mergeAdjacentFreeSlots_doesNothing_whenOnlyOneAdjacentSlot() {
        // Arrange — only the restored BOOKED slot itself is returned; nothing to merge
        final LocalDateTime start = LocalDateTime.of(2026, 5, 10, 10, 0);
        final LocalDateTime end = LocalDateTime.of(2026, 5, 10, 11, 0);
        final UserEntity owner = UserEntity.builder().id(1L).name("Alice").email("alice@example.com").build();
        final TimeslotEntity onlySlot = TimeslotEntity.builder()
            .id(10L).owner(owner).startTime(start).endTime(end).status(SlotBookingStatus.FREE).build();
        when(this.timeslotRepository.findAll(ArgumentMatchers.<Specification<TimeslotEntity>>any()))
            .thenReturn(List.of(onlySlot));

        // Act
        this.timeslotService.mergeAdjacentFreeSlots(1L, start, end);

        // Assert — no deleteAll call; the single slot is left unchanged
        verify(this.timeslotRepository, never()).deleteAll(any());
    }

    @Test
    void mergeAdjacentFreeSlots_mergesAllAdjacentSlots_intoFirstSlot() {
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
        when(this.timeslotRepository.findAll(ArgumentMatchers.<Specification<TimeslotEntity>>any()))
            .thenReturn(List.of(left, middle, right));

        // Act
        this.timeslotService.mergeAdjacentFreeSlots(1L, around, until);

        // Assert — first slot expanded to full range; the other two deleted
        assertThat(left.getStartTime()).isEqualTo(LocalDateTime.of(2026, 5, 10, 9, 0));
        assertThat(left.getEndTime()).isEqualTo(LocalDateTime.of(2026, 5, 10, 12, 0));
        verify(this.timeslotRepository).deleteAll(List.of(middle, right));
    }
}
