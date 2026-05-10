package org.example.meetingscheduler.timeslot;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.example.meetingscheduler.user.UserEntity;
import org.example.meetingscheduler.user.UserRepository;
import org.example.meetingscheduler.user.dto.UserResponseDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
            .organizer(userEntity).startTime(start).endTime(end).status(SlotBookingStatus.FREE).build();
        final TimeslotEntity saved = TimeslotEntity.builder()
            .id(1L).organizer(userEntity).startTime(start).endTime(end).status(SlotBookingStatus.FREE).build();
        final TimeslotResponseDto timeslotResponseDto = new TimeslotResponseDto(
            1L, new UserResponseDto(1L, "Alice", "alice@example.com"), start, end, SlotBookingStatus.FREE);
        when(this.userRepository.findById(1L)).thenReturn(Optional.of(userEntity));
        when(this.timeslotRepository.save(unsaved)).thenReturn(saved);
        when(this.timeslotMapper.toDto(saved)).thenReturn(timeslotResponseDto);

        // Act & Assert
        assertThat(this.timeslotService.createTimeslot(1L, start, end)).isEqualTo(timeslotResponseDto);
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
            .id(1L).organizer(userEntity).startTime(start).endTime(end).status(SlotBookingStatus.FREE).build();
        final TimeslotResponseDto timeslotResponseDto = new TimeslotResponseDto(
            1L, new UserResponseDto(1L, "Alice", "alice@example.com"), start, end, SlotBookingStatus.FREE);
        when(this.timeslotRepository.findAll(ArgumentMatchers.<Specification<TimeslotEntity>>any())).thenReturn(List.of(timeslotEntity));
        when(this.timeslotMapper.toDto(timeslotEntity)).thenReturn(timeslotResponseDto);

        // Act & Assert
        assertThat(this.timeslotService.getTimeslots(1L, null, null, null))
            .containsExactly(timeslotResponseDto);
    }

    @Test
    void getTimeslots_returnsEmptyList_whenNoTimeslots() {
        // Arrange
        when(this.timeslotRepository.findAll(ArgumentMatchers.<Specification<TimeslotEntity>>any())).thenReturn(List.of());

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
            .id(2L).organizer(userEntity).startTime(start).endTime(end).status(SlotBookingStatus.BOOKED).build();
        final TimeslotResponseDto timeslotResponseDto = new TimeslotResponseDto(
            2L, new UserResponseDto(1L, "Alice", "alice@example.com"), start, end, SlotBookingStatus.BOOKED);
        when(this.timeslotRepository.findAll(ArgumentMatchers.<Specification<TimeslotEntity>>any())).thenReturn(List.of(timeslotEntity));
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
            .id(1L).organizer(userEntity)
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
            .id(1L).organizer(userEntity).startTime(start).endTime(end).status(SlotBookingStatus.FREE).build();
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
            .id(1L).organizer(userEntity)
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
        when(this.timeslotRepository.existsById(1L)).thenReturn(true);

        // Act
        this.timeslotService.deleteTimeslot(1L);

        // Assert
        verify(this.timeslotRepository).deleteById(1L);
    }

    @Test
    void deleteTimeslot_throwsNotFound_whenTimeslotDoesNotExist() {
        // Arrange
        when(this.timeslotRepository.existsById(99L)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> this.timeslotService.deleteTimeslot(99L))
            .isInstanceOf(ResponseStatusException.class)
            .extracting(e -> ((ResponseStatusException) e).getStatusCode())
            .isEqualTo(HttpStatus.NOT_FOUND);
    }
}
