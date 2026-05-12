package org.example.meetingscheduler.meeting;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.example.meetingscheduler.meeting.dto.MeetingCreateRequestDto;
import org.example.meetingscheduler.meeting.dto.MeetingResponseDto;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Meetings")
@RestController
@RequiredArgsConstructor
public class MeetingController {

    private final MeetingService meetingService;

    @Operation(
        summary = "List meetings for a user",
        description = "Returns all meetings where the user is the organizer or a participant. Results are sorted by startTime then endTime ascending."
    )
    @ApiResponse(responseCode = "200", description = "OK")
    @GetMapping("/users/{userId}/meetings")
    public List<MeetingResponseDto> getMeetings(@PathVariable final Long userId) {
        return this.meetingService.getMeetings(userId);
    }

    @Operation(
        summary = "Schedule a meeting",
        description = "Every party (organizer and all participants) must have a FREE timeslot fully covering the requested range. Matching slots are split and marked BOOKED."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Meeting created"),
        @ApiResponse(responseCode = "400", description = "Invalid request body or endTime not after startTime"),
        @ApiResponse(responseCode = "409", description = "Meeting with the same organizer and time range already exists"),
        @ApiResponse(responseCode = "422", description = "No covering FREE timeslot for the organizer or a participant")
    })
    @PostMapping("/users/{userId}/meetings")
    @ResponseStatus(HttpStatus.CREATED)
    public MeetingResponseDto createMeeting(@PathVariable final Long userId,
                                            @RequestBody @Valid final MeetingCreateRequestDto meetingCreateRequestDto) {
        return this.meetingService.createMeeting(userId, meetingCreateRequestDto);
    }

    @Operation(
        summary = "Cancel a meeting",
        description = "Restores every party's BOOKED timeslot to FREE and merges any adjacent FREE slots."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Meeting cancelled"),
        @ApiResponse(responseCode = "403", description = "Meeting does not belong to this user"),
        @ApiResponse(responseCode = "404", description = "Meeting not found")
    })
    @DeleteMapping("/users/{userId}/meetings/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMeeting(@PathVariable final Long userId,
                              @PathVariable final Long id) {
        this.meetingService.deleteMeeting(userId, id);
    }
}
