package org.example.meetingscheduler.meeting;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import org.example.meetingscheduler.user.UserResponseDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MeetingServiceTest {

    @InjectMocks
    private MeetingService meetingService;
    @Mock
    private MeetingRepository meetingRepository;
    @Mock
    private MeetingMapper meetingMapper;

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
        final LocalDateTime end   = LocalDateTime.of(2026, 5, 9, 11, 0);
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
}
