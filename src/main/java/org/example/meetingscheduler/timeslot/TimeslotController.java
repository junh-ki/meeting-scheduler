package org.example.meetingscheduler.timeslot;

import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
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

@RestController
@RequiredArgsConstructor
public class TimeslotController {

    private final TimeslotService timeslotService;

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

    @PutMapping("/timeslots/{id}")
    @ResponseStatus(HttpStatus.OK)
    public TimeslotResponseDto updateTimeslot(@PathVariable final Long id,
                                              @RequestBody final TimeslotUpdateRequestDto timeslotUpdateRequestDto) {
        return this.timeslotService.updateTimeslot(
            id,
            timeslotUpdateRequestDto
        );
    }

    @DeleteMapping("/timeslots/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTimeslot(@PathVariable final Long id) {
        this.timeslotService.deleteTimeslot(id);
    }

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
}
