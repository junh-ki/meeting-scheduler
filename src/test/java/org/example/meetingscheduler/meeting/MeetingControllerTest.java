package org.example.meetingscheduler.meeting;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import org.example.meetingscheduler.exception.ConflictException;
import org.example.meetingscheduler.exception.ForbiddenException;
import org.example.meetingscheduler.exception.NotFoundException;
import org.example.meetingscheduler.exception.UnprocessableEntityException;
import org.example.meetingscheduler.meeting.dto.MeetingCreateRequestDto;
import org.example.meetingscheduler.meeting.dto.MeetingResponseDto;
import org.example.meetingscheduler.participant.ParticipantResponseDto;
import org.example.meetingscheduler.user.dto.UserResponseDto;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
        "{\"title\":\"Sync\",\"startTime\":\"2026-05-10T10:00:00\",\"endTime\":\"2026-05-10T11:00:00\",\"participantUserIds\":[2]}";

    @Nested
    class getMeetings {

        @Test
        void returnsEmptyArray_whenNoMeetings() throws Exception {
            // Arrange
            when(meetingService.getMeetings(1L)).thenReturn(Collections.emptyList());

            // Act & Assert
            mockMvc.perform(get("/users/1/meetings"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
        }

        @Test
        void returnsMeetings() throws Exception {
            // Arrange
            final UserResponseDto organizer = new UserResponseDto(1L, "Alice", "alice@example.com");
            final ParticipantResponseDto participant = new ParticipantResponseDto(10L,
                new UserResponseDto(2L, "Bob", "bob@example.com"));
            final MeetingResponseDto meetingResponseDto = new MeetingResponseDto(
                100L, "Sync", "Weekly",
                LocalDateTime.of(2026, 5, 9, 10, 0),
                LocalDateTime.of(2026, 5, 9, 11, 0),
                organizer, List.of(participant));
            when(meetingService.getMeetings(1L)).thenReturn(List.of(meetingResponseDto));

            // Act & Assert
            mockMvc.perform(get("/users/1/meetings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(100))
                .andExpect(jsonPath("$[0].title").value("Sync"))
                .andExpect(jsonPath("$[0].description").value("Weekly"))
                .andExpect(jsonPath("$[0].organizer.name").value("Alice"))
                .andExpect(jsonPath("$[0].participants[0].user.name").value("Bob"));
        }
    }

    @Nested
    class createMeeting {

        @Test
        void returnsCreatedMeeting() throws Exception {
            // Arrange
            final LocalDateTime start = LocalDateTime.of(2026, 5, 10, 10, 0);
            final LocalDateTime end = LocalDateTime.of(2026, 5, 10, 11, 0);
            final MeetingResponseDto meetingResponseDto = new MeetingResponseDto(
                100L, "Sync", "Weekly", start, end,
                new UserResponseDto(1L, "Alice", "alice@example.com"),
                List.of(new ParticipantResponseDto(1L, new UserResponseDto(2L, "Bob", "bob@example.com"))));
            when(meetingService.createMeeting(eq(1L), any(MeetingCreateRequestDto.class))).thenReturn(meetingResponseDto);

            // Act & Assert
            mockMvc.perform(post("/users/1/meetings")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.title").value("Sync"))
                .andExpect(jsonPath("$.organizer.name").value("Alice"))
                .andExpect(jsonPath("$.participants[0].user.name").value("Bob"));
        }

        @Test
        void returnsBadRequest_whenTitleIsBlank() throws Exception {
            // Arrange & Act & Assert
            mockMvc.perform(post("/users/1/meetings")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"title\":\"\",\"startTime\":\"2026-05-10T10:00:00\",\"endTime\":\"2026-05-10T11:00:00\",\"participantUserIds\":[]}"))
                .andExpect(status().isBadRequest());
        }

        @Test
        void returnsBadRequest_whenStartTimeIsNull() throws Exception {
            // Arrange & Act & Assert
            mockMvc.perform(post("/users/1/meetings")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"title\":\"Sync\",\"endTime\":\"2026-05-10T11:00:00\",\"participantUserIds\":[]}"))
                .andExpect(status().isBadRequest());
        }

        @Test
        void returnsBadRequest_whenEndTimeIsNull() throws Exception {
            // Arrange & Act & Assert
            mockMvc.perform(post("/users/1/meetings")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"title\":\"Sync\",\"startTime\":\"2026-05-10T10:00:00\",\"participantUserIds\":[]}"))
                .andExpect(status().isBadRequest());
        }

        @Test
        void returnsBadRequest_whenParticipantUserIdsIsNull() throws Exception {
            // Arrange & Act & Assert
            mockMvc.perform(post("/users/1/meetings")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"title\":\"Sync\",\"startTime\":\"2026-05-10T10:00:00\",\"endTime\":\"2026-05-10T11:00:00\"}"))
                .andExpect(status().isBadRequest());
        }

        @Test
        void returnsUnprocessableEntity_whenNoAvailabilityCoversRange() throws Exception {
            // Arrange
            when(meetingService.createMeeting(eq(1L), any(MeetingCreateRequestDto.class)))
                .thenThrow(new UnprocessableEntityException("No available timeslot covers the requested range"));

            // Act & Assert
            mockMvc.perform(post("/users/1/meetings")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(VALID_BODY))
                .andExpect(status().is(422));
        }

        @Test
        void returnsUnprocessableEntity_whenParticipantHasNoAvailability() throws Exception {
            // Arrange
            when(meetingService.createMeeting(eq(1L), any(MeetingCreateRequestDto.class)))
                .thenThrow(new UnprocessableEntityException("No available timeslot for participant: 2"));

            // Act & Assert
            mockMvc.perform(post("/users/1/meetings")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(VALID_BODY))
                .andExpect(status().is(422));
        }

        @Test
        void returnsConflict_whenMeetingAlreadyExists() throws Exception {
            // Arrange
            when(meetingService.createMeeting(eq(1L), any(MeetingCreateRequestDto.class)))
                .thenThrow(new ConflictException("Meeting already exists for this time range"));

            // Act & Assert
            mockMvc.perform(post("/users/1/meetings")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(VALID_BODY))
                .andExpect(status().isConflict());
        }
    }

    @Nested
    class deleteMeeting {

        @Test
        void returnsNoContent_whenMeetingExists() throws Exception {
            // Arrange
            doNothing().when(meetingService).deleteMeeting(1L, 1L);

            // Act & Assert
            mockMvc.perform(delete("/users/1/meetings/1"))
                .andExpect(status().isNoContent());
        }

        @Test
        void returnsNotFound_whenMeetingDoesNotExist() throws Exception {
            // Arrange
            doThrow(new NotFoundException("Meeting not found"))
                .when(meetingService).deleteMeeting(1L, 99L);

            // Act & Assert
            mockMvc.perform(delete("/users/1/meetings/99"))
                .andExpect(status().isNotFound());
        }

        @Test
        void returnsForbidden_whenUserIsNotOrganizer() throws Exception {
            // Arrange
            doThrow(new ForbiddenException("Meeting does not belong to this user"))
                .when(meetingService).deleteMeeting(2L, 1L);

            // Act & Assert
            mockMvc.perform(delete("/users/2/meetings/1"))
                .andExpect(status().isForbidden());
        }
    }
}
