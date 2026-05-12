package org.example.meetingscheduler.util;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TimeValidationUtilTest {

    @Nested
    class ValidateStartAndEndTime {

        @Test
        void passes_whenStartAndEndTimeAreOnTheSameDay() {
            final LocalDateTime start = LocalDateTime.of(2026, 5, 10, 9, 0);
            final LocalDateTime end   = LocalDateTime.of(2026, 5, 10, 10, 0);
            assertThatCode(() -> TimeValidationUtil.validateStartAndEndTime(start, end))
                .doesNotThrowAnyException();
        }

        @Test
        void throwsBadRequest_whenEndTimeEqualsStartTime() {
            final LocalDateTime start = LocalDateTime.of(2026, 5, 10, 9, 0);
            assertThatThrownBy(() -> TimeValidationUtil.validateStartAndEndTime(start, start))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        void throwsBadRequest_whenEndTimeIsBeforeStartTime() {
            final LocalDateTime start = LocalDateTime.of(2026, 5, 10, 10, 0);
            final LocalDateTime end   = LocalDateTime.of(2026, 5, 10, 9, 0);
            assertThatThrownBy(() -> TimeValidationUtil.validateStartAndEndTime(start, end))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        void throwsBadRequest_whenStartAndEndTimeSpanMidnight() {
            final LocalDateTime start = LocalDateTime.of(2026, 5, 10, 23, 30);
            final LocalDateTime end   = LocalDateTime.of(2026, 5, 11, 0, 30);
            assertThatThrownBy(() -> TimeValidationUtil.validateStartAndEndTime(start, end))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }
}
