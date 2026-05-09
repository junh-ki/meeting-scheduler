package org.example.meetingscheduler.meeting;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class MeetingController {

    private final MeetingService meetingService;

    @GetMapping("/meetings")
    public List<MeetingResponseDto> getMeetings() {
        return this.meetingService.getMeetings();
    }
}
