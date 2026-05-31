package org.example.meetingscheduler.timeslot;

import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.meetingscheduler.exception.ConflictException;
import org.example.meetingscheduler.exception.ForbiddenException;
import org.example.meetingscheduler.exception.NotFoundException;
import org.example.meetingscheduler.timeslot.dto.TimeslotResponseDto;
import org.example.meetingscheduler.timeslot.dto.TimeslotUpdateRequestDto;
import org.example.meetingscheduler.user.UserEntity;
import org.example.meetingscheduler.user.UserRepository;
import org.example.meetingscheduler.util.TimeValidationUtil;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
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
        log.info("Creating timeslot: userId={}, startTime={}, endTime={}", userId, startTime, endTime);
        TimeValidationUtil.validateStartAndEndTime(startTime, endTime);
        final UserEntity owner = this.userRepository.findByIdIs(userId)
            .orElseThrow(() -> new NotFoundException("User not found"));
        validateNotCoveredByExistingSlot(userId, startTime, endTime);
        final List<TimeslotEntity> overlappingTimeslots = this.timeslotRepository.findAll(
            Specification.where(TimeslotSpecifications.hasOwnerId(userId))
                .and(TimeslotSpecifications.overlapsOrAdjacent(startTime, endTime))
        );
        if (!overlappingTimeslots.isEmpty()) {
            if (overlappingTimeslots.stream()
                .anyMatch(overlappingTimeslot -> SlotBookingStatus.BOOKED == overlappingTimeslot.getStatus())) {
                throw new ConflictException("New timeslot overlaps with a BOOKED timeslot");
            }
            final TimeslotResponseDto merged = this.timeslotMapper.toDto(
                mergeInto(overlappingTimeslots, startTime, endTime)
            );
            log.info("Timeslot created (merged): timeslotId={}, userId={}, startTime={}, endTime={}",
                merged.id(), userId, merged.startTime(), merged.endTime());
            return merged;
        }
        final TimeslotResponseDto created = this.timeslotMapper.toDto(
            this.timeslotRepository.save(
                TimeslotEntity.builder()
                    .owner(owner)
                    .startTime(startTime)
                    .endTime(endTime)
                    .status(SlotBookingStatus.FREE)
                    .build()
            )
        );
        log.info("Timeslot created: timeslotId={}, userId={}, startTime={}, endTime={}",
            created.id(), userId, created.startTime(), created.endTime());
        return created;
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
                throw new ConflictException("An existing timeslot already covers this time range");
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
    public TimeslotResponseDto updateTimeslot(final Long userId,
                                              final Long id,
                                              final TimeslotUpdateRequestDto timeslotUpdateRequestDto) {
        log.info("Updating timeslot: timeslotId={}, requestedByUserId={}", id, userId);
        final TimeslotEntity timeslotEntity = this.timeslotRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Timeslot not found"));
        if (!timeslotEntity.getOwner().getId().equals(userId)) {
            throw new ForbiddenException("Timeslot does not belong to this user");
        }
        if (SlotBookingStatus.BOOKED == timeslotEntity.getStatus()) {
            throw new ConflictException("Timeslot is in use by a meeting -- delete the meeting instead");
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
        final TimeslotResponseDto updated = this.timeslotMapper.toDto(timeslotEntity);
        log.info("Timeslot updated: timeslotId={}, userId={}, startTime={}, endTime={}, status={}",
            id, userId, updated.startTime(), updated.endTime(), updated.status());
        return updated;
    }

    /**
     * Pessimistic lock on the timeslot row prevents a concurrent createMeeting from booking it between the FREE status check and the deletion,
     * which would delete a BOOKED timeslot and leave the meeting referencing a non-existent row.
     */
    @Transactional
    public void deleteTimeslot(final Long userId,
                               final Long id) {
        log.info("Deleting timeslot: timeslotId={}, requestedByUserId={}", id, userId);
        final TimeslotEntity timeslotEntity = this.timeslotRepository.findWithLockById(id)
            .orElseThrow(() -> new NotFoundException("Timeslot not found"));
        if (!timeslotEntity.getOwner().getId().equals(userId)) {
            throw new ForbiddenException("Timeslot does not belong to this user");
        }
        if (SlotBookingStatus.BOOKED == timeslotEntity.getStatus()) {
            throw new ConflictException("Timeslot is in use by a meeting -- delete the meeting instead");
        }
        this.timeslotRepository.deleteById(id);
        log.info("Timeslot deleted: timeslotId={}, userId={}", id, userId);
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
