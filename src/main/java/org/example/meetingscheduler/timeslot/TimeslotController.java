package org.example.meetingscheduler.timeslot;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.example.meetingscheduler.timeslot.dto.TimeslotResponseDto;
import org.example.meetingscheduler.timeslot.dto.TimeslotUpdateRequestDto;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Timeslots")
@RestController
@RequiredArgsConstructor
public class TimeslotController {

    private final TimeslotService timeslotService;

    @Operation(
        summary = "Create a timeslot for a user",
        description = "Adjacent or overlapping FREE slots for the same user are automatically merged."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Timeslot created"),
        @ApiResponse(responseCode = "400", description = "endTime must be after startTime"),
        @ApiResponse(responseCode = "404", description = "User not found"),
        @ApiResponse(responseCode = "409", description = "Identical timeslot already exists")
    })
    @PostMapping("/users/{userId}/timeslots")
    @ResponseStatus(HttpStatus.CREATED)
    public TimeslotResponseDto createTimeslot(@PathVariable final Long userId,
                                              @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final LocalDateTime startTime,
                                              @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final LocalDateTime endTime) {
        return this.timeslotService.createTimeslot(
            userId,
            startTime,
            endTime
        );
    }

    @Operation(
        summary = "List timeslots for a user",
        description = "All filters are optional. Results are sorted by startTime then endTime ascending."
    )
    @ApiResponse(responseCode = "200", description = "OK")
    @GetMapping("/users/{userId}/timeslots")
    public List<TimeslotResponseDto> getTimeslots(@PathVariable final Long userId,
                                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final LocalDateTime from,
                                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final LocalDateTime to,
                                                  @RequestParam(required = false) final SlotBookingStatus status) {
        return this.timeslotService.getTimeslots(
            userId,
            from,
            to,
            status
        );
    }

    @Operation(
        summary = "Update a timeslot",
        description = "All fields are optional; omitted fields retain their current values."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Timeslot updated"),
        @ApiResponse(responseCode = "400", description = "Resulting time range is invalid"),
        @ApiResponse(responseCode = "404", description = "Timeslot not found")
    })
    @PutMapping("/timeslots/{id}")
    @ResponseStatus(HttpStatus.OK)
    public TimeslotResponseDto updateTimeslot(@PathVariable final Long id,
                                              @RequestBody @Valid final TimeslotUpdateRequestDto timeslotUpdateRequestDto) {
        return this.timeslotService.updateTimeslot(
            id,
            timeslotUpdateRequestDto
        );
    }

    @Operation(
        summary = "Delete a timeslot",
        description = "BOOKED timeslots cannot be deleted directly; cancel the meeting instead."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Timeslot deleted"),
        @ApiResponse(responseCode = "404", description = "Timeslot not found"),
        @ApiResponse(responseCode = "409", description = "Timeslot is BOOKED by a meeting")
    })
    @DeleteMapping("/timeslots/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTimeslot(@PathVariable final Long id) {
        this.timeslotService.deleteTimeslot(id);
    }
}
