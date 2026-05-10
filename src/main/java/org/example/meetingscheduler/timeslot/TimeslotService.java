package org.example.meetingscheduler.timeslot;

import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.example.meetingscheduler.timeslot.dto.TimeslotResponseDto;
import org.example.meetingscheduler.timeslot.dto.TimeslotUpdateRequestDto;
import org.example.meetingscheduler.user.UserRepository;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class TimeslotService {

    private final UserRepository userRepository;
    private final TimeslotRepository timeslotRepository;
    private final TimeslotMapper timeslotMapper;

    public TimeslotResponseDto createTimeslot(final Long userId,
                                              final LocalDateTime startTime,
                                              final LocalDateTime endTime) {
        validateStartAndEndTime(startTime, endTime);
        return this.timeslotMapper.toDto(
            this.timeslotRepository.save(
                TimeslotEntity.builder()
                    .organizer(this.userRepository.findById(userId).orElseThrow())
                    .startTime(startTime)
                    .endTime(endTime)
                    .status(SlotBookingStatus.FREE)
                    .build()
            )
        );
    }

    @Transactional
    public TimeslotResponseDto updateTimeslot(final Long id,
                                              final TimeslotUpdateRequestDto timeslotUpdateRequestDto) {
        final TimeslotEntity timeslotEntity = this.timeslotRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Timeslot not found"));
        final LocalDateTime effectiveStart = timeslotUpdateRequestDto.startTime() != null
            ? timeslotUpdateRequestDto.startTime()
            : timeslotEntity.getStartTime();
        final LocalDateTime effectiveEnd = timeslotUpdateRequestDto.endTime() != null
            ? timeslotUpdateRequestDto.endTime()
            : timeslotEntity.getEndTime();
        validateStartAndEndTime(effectiveStart, effectiveEnd);
        timeslotEntity.setStartTime(effectiveStart);
        timeslotEntity.setEndTime(effectiveEnd);
        if (timeslotUpdateRequestDto.status() != null) {
            timeslotEntity.setStatus(timeslotUpdateRequestDto.status());
        }
        return this.timeslotMapper.toDto(timeslotEntity);
    }

    private void validateStartAndEndTime(final LocalDateTime startTime,
                                         final LocalDateTime endTime) {
        if (endTime.isAfter(startTime)) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "endTime must be after startTime");
    }

    @Transactional
    public void deleteTimeslot(final Long id) {
        if (!this.timeslotRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Timeslot not found");
        }
        this.timeslotRepository.deleteById(id);
    }

    public List<TimeslotResponseDto> getTimeslots(final Long userId,
                                                  final LocalDateTime from,
                                                  final LocalDateTime to,
                                                  final SlotBookingStatus slotBookingStatus) {
        Specification<TimeslotEntity> specification = Specification.where(TimeslotSpecifications.hasOrganizerId(userId));
        if (from != null) {
            specification = specification.and(TimeslotSpecifications.startTimeFrom(from));
        }
        if (to != null) {
            specification = specification.and(TimeslotSpecifications.endTimeTo(to));
        }
        if (slotBookingStatus != null) {
            specification = specification.and(TimeslotSpecifications.hasStatus(slotBookingStatus));
        }
        return this.timeslotRepository.findAll(specification).stream()
            .map(this.timeslotMapper::toDto)
            .toList();
    }
}
