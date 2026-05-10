package org.example.meetingscheduler.timeslot;

import java.time.LocalDateTime;
import org.example.meetingscheduler.user.dto.UserResponseDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
        final LocalDateTime end   = LocalDateTime.of(2026, 5, 10, 11, 0);
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
}
