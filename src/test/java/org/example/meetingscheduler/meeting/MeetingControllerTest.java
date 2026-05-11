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
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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

    private static final String VALID_BODY =
        "{\"organizerId\":1,\"title\":\"Sync\",\"startTime\":\"2026-05-10T10:00:00\",\"endTime\":\"2026-05-10T11:00:00\",\"participantUserIds\":[2]}";

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
                .content(VALID_BODY))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(100))
            .andExpect(jsonPath("$.title").value("Sync"))
            .andExpect(jsonPath("$.organizer.name").value("Alice"))
            .andExpect(jsonPath("$.participants[0].user.name").value("Bob"));
    }

    @Test
    void createMeeting_returnsBadRequest_whenOrganizerIdIsNull() throws Exception {
        // Arrange & Act & Assert
        this.mockMvc.perform(post("/meetings")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Sync\",\"startTime\":\"2026-05-10T10:00:00\",\"endTime\":\"2026-05-10T11:00:00\",\"participantUserIds\":[]}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void createMeeting_returnsBadRequest_whenTitleIsBlank() throws Exception {
        // Arrange & Act & Assert
        this.mockMvc.perform(post("/meetings")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"organizerId\":1,\"title\":\"\",\"startTime\":\"2026-05-10T10:00:00\",\"endTime\":\"2026-05-10T11:00:00\",\"participantUserIds\":[]}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void createMeeting_returnsBadRequest_whenStartTimeIsNull() throws Exception {
        // Arrange & Act & Assert
        this.mockMvc.perform(post("/meetings")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"organizerId\":1,\"title\":\"Sync\",\"endTime\":\"2026-05-10T11:00:00\",\"participantUserIds\":[]}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void createMeeting_returnsBadRequest_whenEndTimeIsNull() throws Exception {
        // Arrange & Act & Assert
        this.mockMvc.perform(post("/meetings")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"organizerId\":1,\"title\":\"Sync\",\"startTime\":\"2026-05-10T10:00:00\",\"participantUserIds\":[]}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void createMeeting_returnsBadRequest_whenParticipantUserIdsIsNull() throws Exception {
        // Arrange & Act & Assert
        this.mockMvc.perform(post("/meetings")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"organizerId\":1,\"title\":\"Sync\",\"startTime\":\"2026-05-10T10:00:00\",\"endTime\":\"2026-05-10T11:00:00\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void createMeeting_returnsUnprocessableEntity_whenNoAvailabilityCoversRange() throws Exception {
        // Arrange
        when(this.meetingService.createMeeting(any(MeetingCreateRequestDto.class)))
            .thenThrow(new ResponseStatusException(HttpStatusCode.valueOf(422), "No available timeslot covers the requested range"));

        // Act & Assert
        this.mockMvc.perform(post("/meetings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_BODY))
            .andExpect(status().is(422));
    }

    @Test
    void createMeeting_returnsUnprocessableEntity_whenParticipantHasNoAvailability() throws Exception {
        // Arrange
        when(this.meetingService.createMeeting(any(MeetingCreateRequestDto.class)))
            .thenThrow(new ResponseStatusException(HttpStatusCode.valueOf(422), "No available timeslot for participant: 2"));

        // Act & Assert
        this.mockMvc.perform(post("/meetings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_BODY))
            .andExpect(status().is(422));
    }

    @Test
    void deleteMeeting_returnsNoContent_whenMeetingExists() throws Exception {
        // Arrange
        doNothing().when(this.meetingService).deleteMeeting(1L);

        // Act & Assert
        this.mockMvc.perform(delete("/meetings/1"))
            .andExpect(status().isNoContent());
    }

    @Test
    void deleteMeeting_returnsNotFound_whenMeetingDoesNotExist() throws Exception {
        // Arrange
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Meeting not found"))
            .when(this.meetingService).deleteMeeting(99L);

        // Act & Assert
        this.mockMvc.perform(delete("/meetings/99"))
            .andExpect(status().isNotFound());
    }

    @Test
    void createMeeting_returnsConflict_whenMeetingAlreadyExists() throws Exception {
        // Arrange
        when(this.meetingService.createMeeting(any(MeetingCreateRequestDto.class)))
            .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Meeting already exists for this time range"));

        // Act & Assert
        this.mockMvc.perform(post("/meetings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_BODY))
            .andExpect(status().isConflict());
    }
}
