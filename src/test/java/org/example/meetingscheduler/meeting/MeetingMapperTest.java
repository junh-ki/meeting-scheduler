package org.example.meetingscheduler.meeting;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import org.example.meetingscheduler.participant.ParticipantEntity;
import org.example.meetingscheduler.participant.ParticipantResponseDto;
import org.example.meetingscheduler.user.UserEntity;
import org.example.meetingscheduler.user.UserResponseDto;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MeetingMapperTest {

    private static final LocalDateTime START = LocalDateTime.of(2026, 5, 9, 10, 0);
    private static final LocalDateTime END   = LocalDateTime.of(2026, 5, 9, 11, 0);
    private final MeetingMapper meetingMapper = new MeetingMapperImpl();

    @Test
    void toDto_mapsAllFields() {
        // Arrange
        final UserEntity organizer = UserEntity.builder()
            .id(1L)
            .name("Alice")
            .email("alice@example.com")
            .build();
        final UserEntity participantUser = UserEntity.builder()
            .id(2L)
            .name("Bob")
            .email("bob@example.com")
            .build();
        final ParticipantEntity participant = ParticipantEntity.builder()
            .id(10L)
            .user(participantUser)
            .build();
        final MeetingEntity meetingEntity = MeetingEntity.builder()
            .id(100L)
            .title("Sync")
            .description("Weekly")
            .startTime(START)
            .endTime(END)
            .organizer(organizer)
            .participants(List.of(participant))
            .build();

        // Act
        final MeetingResponseDto meetingMapperDto = this.meetingMapper.toDto(meetingEntity);

        // Assert
        assertThat(meetingMapperDto.id()).isEqualTo(100L);
        assertThat(meetingMapperDto.title()).isEqualTo("Sync");
        assertThat(meetingMapperDto.description()).isEqualTo("Weekly");
        assertThat(meetingMapperDto.startTime()).isEqualTo(START);
        assertThat(meetingMapperDto.endTime()).isEqualTo(END);
        assertThat(meetingMapperDto.organizer()).isEqualTo(new UserResponseDto(1L, "Alice", "alice@example.com"));
        assertThat(meetingMapperDto.participants()).containsExactly(
            new ParticipantResponseDto(10L, new UserResponseDto(2L, "Bob", "bob@example.com")));
    }

    @Test
    void toDto_nullOrganizer_mapsToNull() {
        // Arrange
        final MeetingEntity meetingEntity = MeetingEntity.builder()
            .id(1L)
            .title("Sync")
            .startTime(START)
            .endTime(END)
            .participants(Collections.emptyList())
            .build();

        // Act & Assert
        assertThat(this.meetingMapper.toDto(meetingEntity).organizer()).isNull();
    }

    @Test
    void toDto_emptyParticipants_mapsToEmptyList() {
        // Arrange
        final MeetingEntity meetingEntity = MeetingEntity.builder()
            .id(1L)
            .title("Sync")
            .startTime(START)
            .endTime(END)
            .participants(Collections.emptyList())
            .build();

        // Act & Assert
        assertThat(this.meetingMapper.toDto(meetingEntity).participants()).isEmpty();
    }
}
