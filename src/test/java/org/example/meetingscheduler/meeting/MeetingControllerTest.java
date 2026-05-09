package org.example.meetingscheduler.meeting;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import org.example.meetingscheduler.participant.ParticipantResponseDto;
import org.example.meetingscheduler.user.UserResponseDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MeetingController.class)
class MeetingControllerTest {

    @MockitoBean
    private MeetingService meetingService;
    @Autowired
    private MockMvc mockMvc;

    @Test
    void getMeetings_returnsEmptyArray_whenNoMeetings() throws Exception {
        // Arrange
        when(this.meetingService.getMeetings()).thenReturn(Collections.emptyList());

        // Act & Assert
        this.mockMvc.perform(get("/meetings"))
            .andExpect(status().isOk())
            .andExpect(content().json("[]"));
    }

    @Test
    void getMeetings_returnsMeetings() throws Exception {
        // Arrange
        final UserResponseDto organizer = new UserResponseDto(1L, "Alice", "alice@example.com");
        final ParticipantResponseDto participant = new ParticipantResponseDto(10L,
            new UserResponseDto(2L, "Bob", "bob@example.com"));
        final MeetingResponseDto meetingResponseDto = new MeetingResponseDto(
            100L, "Sync", "Weekly",
            LocalDateTime.of(2026, 5, 9, 10, 0),
            LocalDateTime.of(2026, 5, 9, 11, 0),
            organizer, List.of(participant));
        when(this.meetingService.getMeetings()).thenReturn(List.of(meetingResponseDto));

        // Act & Assert
        this.mockMvc.perform(get("/meetings"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(100))
            .andExpect(jsonPath("$[0].title").value("Sync"))
            .andExpect(jsonPath("$[0].description").value("Weekly"))
            .andExpect(jsonPath("$[0].organizer.name").value("Alice"))
            .andExpect(jsonPath("$[0].participants[0].user.name").value("Bob"));
    }
}
