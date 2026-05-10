package org.example.meetingscheduler.timeslot;

import java.time.LocalDateTime;
import org.example.meetingscheduler.user.UserEntity;
import org.example.meetingscheduler.user.dto.UserResponseDto;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TimeslotMapperTest {

    private static final LocalDateTime START = LocalDateTime.of(2026, 5, 10, 10, 0);
    private static final LocalDateTime END   = LocalDateTime.of(2026, 5, 10, 11, 0);
    private final TimeslotMapper timeslotMapper = new TimeslotMapperImpl();

    @Test
    void toDto_mapsAllFields() {
        // Arrange
        final UserEntity organizer = UserEntity.builder()
            .id(1L)
            .name("Alice")
            .email("alice@example.com")
            .build();
        final TimeslotEntity timeslotEntity = TimeslotEntity.builder()
            .id(1L)
            .organizer(organizer)
            .startTime(START)
            .endTime(END)
            .status(SlotBookingStatus.FREE)
            .build();

        // Act
        final TimeslotResponseDto dto = this.timeslotMapper.toDto(timeslotEntity);

        // Assert
        assertThat(dto.id()).isEqualTo(1L);
        assertThat(dto.organizer()).isEqualTo(new UserResponseDto(1L, "Alice", "alice@example.com"));
        assertThat(dto.startTime()).isEqualTo(START);
        assertThat(dto.endTime()).isEqualTo(END);
        assertThat(dto.status()).isEqualTo(SlotBookingStatus.FREE);
    }

    @Test
    void toDto_nullOrganizer_mapsToNull() {
        // Arrange
        final TimeslotEntity timeslotEntity = TimeslotEntity.builder()
            .id(1L)
            .startTime(START)
            .endTime(END)
            .status(SlotBookingStatus.FREE)
            .build();

        // Act & Assert
        assertThat(this.timeslotMapper.toDto(timeslotEntity).organizer()).isNull();
    }
}
