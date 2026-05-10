package org.example.meetingscheduler.timeslot;

import java.time.LocalDateTime;
import java.util.List;
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

    @Test
    void createTimeslot_returnsCreatedTimeslot() throws Exception {
        // Arrange
        final LocalDateTime start = LocalDateTime.of(2026, 5, 10, 10, 0);
        final LocalDateTime end = LocalDateTime.of(2026, 5, 10, 11, 0);
        final TimeslotResponseDto timeslotResponseDto = new TimeslotResponseDto(
            1L, new UserResponseDto(1L, "Alice", "alice@example.com"), start, end, SlotBookingStatus.FREE);
        when(this.timeslotService.createTimeslot(1L, start, end)).thenReturn(timeslotResponseDto);

        // Act & Assert
        this.mockMvc.perform(post("/users/1/timeslots")
                .param("startTime", "2026-05-10T10:00:00")
                .param("endTime", "2026-05-10T11:00:00"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.organizer.name").value("Alice"))
            .andExpect(jsonPath("$.status").value("FREE"));
    }

    @Test
    void createTimeslot_returnsBadRequest_whenServiceRejectsTimeRange() throws Exception {
        // Arrange
        when(this.timeslotService.createTimeslot(any(), any(), any()))
            .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "endTime must be after startTime"));

        // Act & Assert
        this.mockMvc.perform(post("/users/1/timeslots")
                .param("startTime", "2026-05-10T10:00:00")
                .param("endTime", "2026-05-10T10:00:00"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void getTimeslots_returnsTimeslots_whenNoFilters() throws Exception {
        // Arrange
        final LocalDateTime start = LocalDateTime.of(2026, 5, 10, 10, 0);
        final LocalDateTime end = LocalDateTime.of(2026, 5, 10, 11, 0);
        final TimeslotResponseDto timeslotResponseDto = new TimeslotResponseDto(
            1L, new UserResponseDto(1L, "Alice", "alice@example.com"), start, end, SlotBookingStatus.FREE);
        when(this.timeslotService.getTimeslots(eq(1L), isNull(), isNull(), isNull()))
            .thenReturn(List.of(timeslotResponseDto));

        // Act & Assert
        this.mockMvc.perform(get("/users/1/timeslots"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(1))
            .andExpect(jsonPath("$[0].organizer.name").value("Alice"))
            .andExpect(jsonPath("$[0].status").value("FREE"));
    }

    @Test
    void getTimeslots_returnsEmptyArray_whenNoTimeslots() throws Exception {
        // Arrange
        when(this.timeslotService.getTimeslots(eq(1L), isNull(), isNull(), isNull()))
            .thenReturn(List.of());

        // Act & Assert
        this.mockMvc.perform(get("/users/1/timeslots"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void getTimeslots_withAllFilters_returnsFilteredTimeslots() throws Exception {
        // Arrange
        final LocalDateTime start = LocalDateTime.of(2026, 5, 10, 10, 0);
        final LocalDateTime end = LocalDateTime.of(2026, 5, 10, 11, 0);
        final TimeslotResponseDto timeslotResponseDto = new TimeslotResponseDto(
            2L, new UserResponseDto(1L, "Alice", "alice@example.com"), start, end, SlotBookingStatus.BOOKED);
        when(this.timeslotService.getTimeslots(eq(1L), any(), any(), eq(SlotBookingStatus.BOOKED)))
            .thenReturn(List.of(timeslotResponseDto));

        // Act & Assert
        this.mockMvc.perform(get("/users/1/timeslots")
                .param("from", "2026-05-10T00:00:00")
                .param("to", "2026-05-10T23:59:59")
                .param("status", "BOOKED"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(2))
            .andExpect(jsonPath("$[0].status").value("BOOKED"));
    }

    @Test
    void updateTimeslot_returnsUpdatedTimeslot() throws Exception {
        // Arrange
        final LocalDateTime start = LocalDateTime.of(2026, 5, 10, 11, 0);
        final LocalDateTime end = LocalDateTime.of(2026, 5, 10, 12, 0);
        final TimeslotResponseDto timeslotResponseDto = new TimeslotResponseDto(
            1L, new UserResponseDto(1L, "Alice", "alice@example.com"), start, end, SlotBookingStatus.BOOKED);
        when(this.timeslotService.updateTimeslot(eq(1L), any(TimeslotUpdateRequestDto.class))).thenReturn(timeslotResponseDto);

        // Act & Assert
        this.mockMvc.perform(put("/timeslots/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"startTime\":\"2026-05-10T11:00:00\",\"endTime\":\"2026-05-10T12:00:00\",\"status\":\"BOOKED\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.status").value("BOOKED"));
    }

    @Test
    void updateTimeslot_returnsNotFound_whenTimeslotDoesNotExist() throws Exception {
        // Arrange
        when(this.timeslotService.updateTimeslot(eq(99L), any(TimeslotUpdateRequestDto.class)))
            .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Timeslot not found"));

        // Act & Assert
        this.mockMvc.perform(put("/timeslots/99")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"BOOKED\"}"))
            .andExpect(status().isNotFound());
    }

    @Test
    void deleteTimeslot_returnsNoContent() throws Exception {
        // Act & Assert
        this.mockMvc.perform(delete("/timeslots/1"))
            .andExpect(status().isNoContent());
    }

    @Test
    void deleteTimeslot_returnsNotFound_whenTimeslotDoesNotExist() throws Exception {
        // Arrange
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Timeslot not found"))
            .when(this.timeslotService).deleteTimeslot(99L);

        // Act & Assert
        this.mockMvc.perform(delete("/timeslots/99"))
            .andExpect(status().isNotFound());
    }
}
