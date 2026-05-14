package org.example.meetingscheduler.util;

import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.example.meetingscheduler.exception.BadRequestException;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TimeValidationUtil {

    public static void validateStartAndEndTime(final LocalDateTime startTime,
                                               final LocalDateTime endTime) {
        if (!endTime.isAfter(startTime)) {
            throw new BadRequestException("endTime must be after startTime");
        }
        if (!startTime.toLocalDate().equals(endTime.toLocalDate())) {
            throw new BadRequestException("startTime and endTime must be on the same date");
        }
    }
}
