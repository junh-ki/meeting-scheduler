package org.example.meetingscheduler.meeting;

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

@RestController
@RequiredArgsConstructor
public class MeetingController {

    private final MeetingService meetingService;

    @GetMapping("/meetings")
    public List<MeetingResponseDto> getMeetings() {
        return this.meetingService.getMeetings();
    }

    @PostMapping("/meetings")
    @ResponseStatus(HttpStatus.CREATED)
    public MeetingResponseDto createMeeting(@RequestBody @Valid final MeetingCreateRequestDto meetingCreateRequestDto) {
        return this.meetingService.createMeeting(meetingCreateRequestDto);
    }

    @DeleteMapping("/meetings/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMeeting(@PathVariable final Long id) {
        this.meetingService.deleteMeeting(id);
    }
}
