package org.example.meetingscheduler.meeting;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import org.example.meetingscheduler.meeting.dto.MeetingCreateRequestDto;
import org.example.meetingscheduler.meeting.dto.MeetingResponseDto;
import org.example.meetingscheduler.participant.ParticipantResponseDto;
import org.example.meetingscheduler.user.dto.UserResponseDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

    @Test
    void createMeeting_returnsCreatedMeeting() throws Exception {
        // Arrange
        final LocalDateTime start = LocalDateTime.of(2026, 5, 10, 10, 0);
        final LocalDateTime end = LocalDateTime.of(2026, 5, 10, 11, 0);
        final MeetingResponseDto meetingResponseDto = new MeetingResponseDto(
            100L, "Sync", "Weekly", start, end,
            new UserResponseDto(1L, "Alice", "alice@example.com"),
            List.of(new ParticipantResponseDto(1L, new UserResponseDto(2L, "Bob", "bob@example.com"))));
        when(this.meetingService.createMeeting(any(MeetingCreateRequestDto.class))).thenReturn(meetingResponseDto);

        // Act & Assert
        this.mockMvc.perform(post("/meetings")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"timeslotId\":10,\"title\":\"Sync\",\"description\":\"Weekly\",\"participantUserIds\":[2]}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(100))
            .andExpect(jsonPath("$.title").value("Sync"))
            .andExpect(jsonPath("$.organizer.name").value("Alice"))
            .andExpect(jsonPath("$.participants[0].user.name").value("Bob"));
    }

    @Test
    void createMeeting_returnsBadRequest_whenTimeslotIdIsNull() throws Exception {
        // Arrange & Act & Assert
        this.mockMvc.perform(post("/meetings")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Sync\",\"participantUserIds\":[]}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void createMeeting_returnsBadRequest_whenTitleIsBlank() throws Exception {
        // Arrange & Act & Assert
        this.mockMvc.perform(post("/meetings")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"timeslotId\":10,\"title\":\"\",\"participantUserIds\":[]}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void createMeeting_returnsBadRequest_whenParticipantUserIdsIsNull() throws Exception {
        // Arrange & Act & Assert
        this.mockMvc.perform(post("/meetings")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"timeslotId\":10,\"title\":\"Sync\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void createMeeting_returnsNotFound_whenTimeslotDoesNotExist() throws Exception {
        // Arrange
        when(this.meetingService.createMeeting(any(MeetingCreateRequestDto.class)))
            .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Timeslot not found"));

        // Act & Assert
        this.mockMvc.perform(post("/meetings")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"timeslotId\":99,\"title\":\"Sync\",\"participantUserIds\":[]}"))
            .andExpect(status().isNotFound());
    }

    @Test
    void createMeeting_returnsConflict_whenTimeslotIsNotFree() throws Exception {
        // Arrange
        when(this.meetingService.createMeeting(any(MeetingCreateRequestDto.class)))
            .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Timeslot is not available"));

        // Act & Assert
        this.mockMvc.perform(post("/meetings")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"timeslotId\":10,\"title\":\"Sync\",\"participantUserIds\":[]}"))
            .andExpect(status().isConflict());
    }
}
