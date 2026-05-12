package org.example.meetingscheduler.timeslot;

import java.time.LocalDateTime;
import java.util.List;
import org.example.meetingscheduler.timeslot.dto.TimeslotResponseDto;
import org.example.meetingscheduler.timeslot.dto.TimeslotUpdateRequestDto;
import org.example.meetingscheduler.user.dto.UserResponseDto;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TimeslotController.class)
class TimeslotControllerTest {

    @MockitoBean
    private TimeslotService timeslotService;
    @Autowired
    private MockMvc mockMvc;

    @Nested
    class createTimeslot {

        @Test
        void returnsCreatedTimeslot() throws Exception {
            // Arrange
            final LocalDateTime start = LocalDateTime.of(2026, 5, 10, 10, 0);
            final LocalDateTime end = LocalDateTime.of(2026, 5, 10, 11, 0);
            final TimeslotResponseDto timeslotResponseDto = new TimeslotResponseDto(
                1L, new UserResponseDto(1L, "Alice", "alice@example.com"), start, end, SlotBookingStatus.FREE);
            when(timeslotService.createTimeslot(1L, start, end)).thenReturn(timeslotResponseDto);

            // Act & Assert
            mockMvc.perform(post("/users/1/timeslots")
                    .param("startTime", "2026-05-10T10:00:00")
                    .param("endTime", "2026-05-10T11:00:00"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.owner.name").value("Alice"))
                .andExpect(jsonPath("$.status").value("FREE"));
        }

        @Test
        void returnsConflict_whenTimeslotAlreadyExists() throws Exception {
            // Arrange
            when(timeslotService.createTimeslot(any(), any(), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Timeslot already exists for this time range"));

            // Act & Assert
            mockMvc.perform(post("/users/1/timeslots")
                    .param("startTime", "2026-05-10T10:00:00")
                    .param("endTime", "2026-05-10T11:00:00"))
                .andExpect(status().isConflict());
        }

        @Test
        void returnsBadRequest_whenServiceRejectsTimeRange() throws Exception {
            // Arrange
            when(timeslotService.createTimeslot(any(), any(), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "endTime must be after startTime"));

            // Act & Assert
            mockMvc.perform(post("/users/1/timeslots")
                    .param("startTime", "2026-05-10T10:00:00")
                    .param("endTime", "2026-05-10T10:00:00"))
                .andExpect(status().isBadRequest());
        }
    }

    @Nested
    class getTimeslots {

        @Test
        void returnsTimeslots_whenNoFilters() throws Exception {
            // Arrange
            final LocalDateTime start = LocalDateTime.of(2026, 5, 10, 10, 0);
            final LocalDateTime end = LocalDateTime.of(2026, 5, 10, 11, 0);
            final TimeslotResponseDto timeslotResponseDto = new TimeslotResponseDto(
                1L, new UserResponseDto(1L, "Alice", "alice@example.com"), start, end, SlotBookingStatus.FREE);
            when(timeslotService.getTimeslots(eq(1L), isNull(), isNull(), isNull()))
                .thenReturn(List.of(timeslotResponseDto));

            // Act & Assert
            mockMvc.perform(get("/users/1/timeslots"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].owner.name").value("Alice"))
                .andExpect(jsonPath("$[0].status").value("FREE"));
        }

        @Test
        void returnsEmptyArray_whenNoTimeslots() throws Exception {
            // Arrange
            when(timeslotService.getTimeslots(eq(1L), isNull(), isNull(), isNull()))
                .thenReturn(List.of());

            // Act & Assert
            mockMvc.perform(get("/users/1/timeslots"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
        }

        @Test
        void withAllFilters_returnsFilteredTimeslots() throws Exception {
            // Arrange
            final LocalDateTime start = LocalDateTime.of(2026, 5, 10, 10, 0);
            final LocalDateTime end = LocalDateTime.of(2026, 5, 10, 11, 0);
            final TimeslotResponseDto timeslotResponseDto = new TimeslotResponseDto(
                2L, new UserResponseDto(1L, "Alice", "alice@example.com"), start, end, SlotBookingStatus.BOOKED);
            when(timeslotService.getTimeslots(eq(1L), any(), any(), eq(SlotBookingStatus.BOOKED)))
                .thenReturn(List.of(timeslotResponseDto));

            // Act & Assert
            mockMvc.perform(get("/users/1/timeslots")
                    .param("from", "2026-05-10T00:00:00")
                    .param("to", "2026-05-10T23:59:59")
                    .param("status", "BOOKED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(2))
                .andExpect(jsonPath("$[0].status").value("BOOKED"));
        }
    }

    @Nested
    class updateTimeslot {

        @Test
        void returnsUpdatedTimeslot() throws Exception {
            // Arrange
            final LocalDateTime start = LocalDateTime.of(2026, 5, 10, 11, 0);
            final LocalDateTime end = LocalDateTime.of(2026, 5, 10, 12, 0);
            final TimeslotResponseDto timeslotResponseDto = new TimeslotResponseDto(
                1L, new UserResponseDto(1L, "Alice", "alice@example.com"), start, end, SlotBookingStatus.BOOKED);
            when(timeslotService.updateTimeslot(eq(1L), any(TimeslotUpdateRequestDto.class))).thenReturn(timeslotResponseDto);

            // Act & Assert
            mockMvc.perform(put("/timeslots/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"startTime\":\"2026-05-10T11:00:00\",\"endTime\":\"2026-05-10T12:00:00\",\"status\":\"BOOKED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("BOOKED"));
        }

        @Test
        void returnsNotFound_whenTimeslotDoesNotExist() throws Exception {
            // Arrange
            when(timeslotService.updateTimeslot(eq(99L), any(TimeslotUpdateRequestDto.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Timeslot not found"));

            // Act & Assert
            mockMvc.perform(put("/timeslots/99")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"status\":\"BOOKED\"}"))
                .andExpect(status().isNotFound());
        }

        @Test
        void returnsConflict_whenTimeslotIsBooked() throws Exception {
            // Arrange
            when(timeslotService.updateTimeslot(eq(10L), any(TimeslotUpdateRequestDto.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Timeslot is in use by a meeting -- cancel the meeting instead"));

            // Act & Assert
            mockMvc.perform(put("/timeslots/10")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"status\":\"FREE\"}"))
                .andExpect(status().isConflict());
        }
    }

    @Nested
    class deleteTimeslot {

        @Test
        void returnsNoContent() throws Exception {
            // Act & Assert
            mockMvc.perform(delete("/timeslots/1"))
                .andExpect(status().isNoContent());
        }

        @Test
        void returnsNotFound_whenTimeslotDoesNotExist() throws Exception {
            // Arrange
            doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Timeslot not found"))
                .when(timeslotService).deleteTimeslot(99L);

            // Act & Assert
            mockMvc.perform(delete("/timeslots/99"))
                .andExpect(status().isNotFound());
        }

        @Test
        void returnsConflict_whenTimeslotIsBooked() throws Exception {
            // Arrange
            doThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Timeslot is in use by a meeting -- cancel the meeting instead"))
                .when(timeslotService).deleteTimeslot(10L);

            // Act & Assert
            mockMvc.perform(delete("/timeslots/10"))
                .andExpect(status().isConflict());
        }
    }
}
