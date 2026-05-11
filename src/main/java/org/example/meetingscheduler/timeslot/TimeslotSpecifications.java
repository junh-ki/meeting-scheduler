package org.example.meetingscheduler.timeslot;

import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.Specification;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TimeslotSpecifications {

    public static Specification<TimeslotEntity> hasOrganizerId(final Long userId) {
        return (root, criteriaQuery, criteriaBuilder) ->
            criteriaBuilder.equal(root.get("organizer").get("id"), userId);
    }

    public static Specification<TimeslotEntity> startTimeFrom(final LocalDateTime from) {
        return (root, criteriaQuery, criteriaBuilder) ->
            criteriaBuilder.greaterThanOrEqualTo(root.get("startTime"), from);
    }

    public static Specification<TimeslotEntity> endTimeTo(final LocalDateTime to) {
        return (root, criteriaQuery, criteriaBuilder) ->
            criteriaBuilder.lessThanOrEqualTo(root.get("endTime"), to);
    }

    public static Specification<TimeslotEntity> hasStatus(final SlotBookingStatus slotBookingStatus) {
        return (root, criteriaQuery, criteriaBuilder) ->
            criteriaBuilder.equal(root.get("status"), slotBookingStatus);
    }

    public static Specification<TimeslotEntity> overlapsOrAdjacent(final LocalDateTime startTime,
                                                                   final LocalDateTime endTime) {
        return (root, criteriaQuery, criteriaBuilder) ->
            criteriaBuilder.and(
                criteriaBuilder.lessThanOrEqualTo(root.get("startTime"), endTime),
                criteriaBuilder.greaterThanOrEqualTo(root.get("endTime"), startTime)
            );
    }

    public static Specification<TimeslotEntity> coversRange(final LocalDateTime startTime,
                                                            final LocalDateTime endTime) {
        return (root, criteriaQuery, criteriaBuilder) ->
            criteriaBuilder.and(
                criteriaBuilder.lessThanOrEqualTo(root.get("startTime"), startTime),
                criteriaBuilder.greaterThanOrEqualTo(root.get("endTime"), endTime)
            );
    }
}
