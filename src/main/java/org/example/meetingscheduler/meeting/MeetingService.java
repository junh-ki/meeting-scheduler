package org.example.meetingscheduler.meeting;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MeetingService {

    private final MeetingRepository meetingRepository;
    private final MeetingMapper meetingMapper;

    public List<MeetingResponseDto> getMeetings() {
        return this.meetingRepository.findAll()
            .stream()
            .map(this.meetingMapper::toDto)
            .toList();
    }
}
