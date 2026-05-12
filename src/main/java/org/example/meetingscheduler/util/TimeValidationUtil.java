package org.example.meetingscheduler.util;

import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TimeValidationUtil {

    public static void validateStartAndEndTime(final LocalDateTime startTime,
                                               final LocalDateTime endTime) {
        if (!endTime.isAfter(startTime)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "endTime must be after startTime");
        }
        if (!startTime.toLocalDate().equals(endTime.toLocalDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "startTime and endTime must be on the same date");
        }
    }
}
