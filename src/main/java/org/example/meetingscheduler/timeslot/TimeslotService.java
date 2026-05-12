package org.example.meetingscheduler.timeslot;

import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.example.meetingscheduler.timeslot.dto.TimeslotResponseDto;
import org.example.meetingscheduler.timeslot.dto.TimeslotUpdateRequestDto;
import org.example.meetingscheduler.user.UserEntity;
import org.example.meetingscheduler.user.UserRepository;
import org.example.meetingscheduler.util.TimeValidationUtil;
import org.springframework.data.domain.Sort;
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

    @Transactional
    public TimeslotResponseDto createTimeslot(final Long userId,
                                              final LocalDateTime startTime,
                                              final LocalDateTime endTime) {
        TimeValidationUtil.validateStartAndEndTime(startTime, endTime);
        final UserEntity owner = this.userRepository.findByIdIs(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        validateNotCoveredByExistingSlot(userId, startTime, endTime);
        final List<TimeslotEntity> overlapping = this.timeslotRepository.findAll(
            Specification.where(TimeslotSpecifications.hasOwnerId(userId))
                .and(TimeslotSpecifications.overlapsOrAdjacent(startTime, endTime))
        );
        if (!overlapping.isEmpty()) {
            return this.timeslotMapper.toDto(
                mergeInto(overlapping, startTime, endTime)
            );
        }
        return this.timeslotMapper.toDto(
            this.timeslotRepository.save(
                TimeslotEntity.builder()
                    .owner(owner)
                    .startTime(startTime)
                    .endTime(endTime)
                    .status(SlotBookingStatus.FREE)
                    .build()
            )
        );
    }

    private void validateNotCoveredByExistingSlot(final Long userId,
                                                  final LocalDateTime startTime,
                                                  final LocalDateTime endTime) {
        this.timeslotRepository
            .findAll(
                Specification
                    .where(TimeslotSpecifications.hasOwnerId(userId))
                    .and(TimeslotSpecifications.coversRange(startTime, endTime))
            ).stream()
            .findFirst()
            .ifPresent(coveringTimeslot -> {
                throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "An existing timeslot already covers this time range"
                );
            });
    }

    /**
     * Called by MeetingService after restoring a timeslot to FREE on meeting deletion
     */
    public void mergeAdjacentFreeSlots(final Long userId,
                                       final LocalDateTime start,
                                       final LocalDateTime end) {
        final List<TimeslotEntity> adjacentTimeslots = this.timeslotRepository.findAll(
            Specification.where(
                TimeslotSpecifications
                    .hasOwnerId(userId))
                    .and(TimeslotSpecifications.hasStatus(SlotBookingStatus.FREE))
                    .and(TimeslotSpecifications.overlapsOrAdjacent(start, end))
        );
        if (adjacentTimeslots.size() > 1) {
            mergeInto(adjacentTimeslots, start, end);
        }
    }

    /**
     * newStart/newEnd are used as seeds so the new range is always included in the merged bounds
     */
    private TimeslotEntity mergeInto(final List<TimeslotEntity> overlappingTimeslots,
                                     final LocalDateTime newStart,
                                     final LocalDateTime newEnd) {
        final LocalDateTime mergedStart = overlappingTimeslots.stream()
            .map(TimeslotEntity::getStartTime)
            .reduce(newStart, (a, b) -> a.isBefore(b) ? a : b);
        final LocalDateTime mergedEnd = overlappingTimeslots.stream()
            .map(TimeslotEntity::getEndTime)
            .reduce(newEnd, (a, b) -> a.isAfter(b) ? a : b);
        final TimeslotEntity primary = overlappingTimeslots.getFirst();
        primary.setStartTime(mergedStart);
        primary.setEndTime(mergedEnd);
        if (overlappingTimeslots.size() > 1) {
            this.timeslotRepository.deleteAll(overlappingTimeslots.subList(1, overlappingTimeslots.size()));
        }
        return primary;
    }

    @Transactional
    public TimeslotResponseDto updateTimeslot(final Long id,
                                              final TimeslotUpdateRequestDto timeslotUpdateRequestDto) {
        final TimeslotEntity timeslotEntity = this.timeslotRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Timeslot not found"));
        if (SlotBookingStatus.BOOKED == timeslotEntity.getStatus()) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Timeslot is in use by a meeting -- cancel the meeting instead"
            );
        }
        final LocalDateTime effectiveStart = timeslotUpdateRequestDto.startTime() != null
            ? timeslotUpdateRequestDto.startTime()
            : timeslotEntity.getStartTime();
        final LocalDateTime effectiveEnd = timeslotUpdateRequestDto.endTime() != null
            ? timeslotUpdateRequestDto.endTime()
            : timeslotEntity.getEndTime();
        TimeValidationUtil.validateStartAndEndTime(effectiveStart, effectiveEnd);
        timeslotEntity.setStartTime(effectiveStart);
        timeslotEntity.setEndTime(effectiveEnd);
        if (timeslotUpdateRequestDto.status() != null) {
            timeslotEntity.setStatus(timeslotUpdateRequestDto.status());
        }
        return this.timeslotMapper.toDto(timeslotEntity);
    }

    @Transactional
    public void deleteTimeslot(final Long id) {
        final TimeslotEntity timeslotEntity = this.timeslotRepository.findById(id)
            .orElseThrow(() ->
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Timeslot not found"
                )
            );
        if (SlotBookingStatus.BOOKED == timeslotEntity.getStatus()) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Timeslot is in use by a meeting -- cancel the meeting instead"
            );
        }
        this.timeslotRepository.deleteById(id);
    }

    public List<TimeslotResponseDto> getTimeslots(final Long userId,
                                                  final LocalDateTime from,
                                                  final LocalDateTime to,
                                                  final SlotBookingStatus slotBookingStatus) {
        Specification<TimeslotEntity> specification = Specification.where(TimeslotSpecifications.hasOwnerId(userId));
        if (from != null) {
            specification = specification.and(TimeslotSpecifications.startTimeFrom(from));
        }
        if (to != null) {
            specification = specification.and(TimeslotSpecifications.endTimeTo(to));
        }
        if (slotBookingStatus != null) {
            specification = specification.and(TimeslotSpecifications.hasStatus(slotBookingStatus));
        }
        return this.timeslotRepository
            .findAll(
                specification,
                Sort.by(
                    Sort.Order.asc("startTime"),
                    Sort.Order.asc("endTime")
                )
            ).stream()
            .map(this.timeslotMapper::toDto)
            .toList();
    }
}
