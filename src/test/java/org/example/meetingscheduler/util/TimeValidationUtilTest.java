package org.example.meetingscheduler.util;

import java.time.LocalDateTime;
import org.example.meetingscheduler.exception.BadRequestException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TimeValidationUtilTest {

    @Test
    void passes_whenStartAndEndTimeAreOnTheSameDay() {
        // Arrange
        final LocalDateTime start = LocalDateTime.of(2026, 5, 10, 9, 0);
        final LocalDateTime end = LocalDateTime.of(2026, 5, 10, 10, 0);

        // Act & Assert
        assertThatCode(() -> TimeValidationUtil.validateStartAndEndTime(start, end))
            .doesNotThrowAnyException();
    }

    @Test
    void throwsBadRequest_whenEndTimeEqualsStartTime() {
        // Arrange
        final LocalDateTime start = LocalDateTime.of(2026, 5, 10, 9, 0);

        // Act & Assert
        assertThatThrownBy(() -> TimeValidationUtil.validateStartAndEndTime(start, start))
            .isInstanceOf(BadRequestException.class);
    }

    @Test
    void throwsBadRequest_whenEndTimeIsBeforeStartTime() {
        // Arrange
        final LocalDateTime start = LocalDateTime.of(2026, 5, 10, 10, 0);
        final LocalDateTime end = LocalDateTime.of(2026, 5, 10, 9, 0);

        // Act & Assert
        assertThatThrownBy(() -> TimeValidationUtil.validateStartAndEndTime(start, end))
            .isInstanceOf(BadRequestException.class);
    }

    @Test
    void throwsBadRequest_whenStartAndEndTimeSpanMidnight() {
        // Arrange
        final LocalDateTime start = LocalDateTime.of(2026, 5, 10, 23, 30);
        final LocalDateTime end = LocalDateTime.of(2026, 5, 11, 0, 30);

        // Act & Assert
        assertThatThrownBy(() -> TimeValidationUtil.validateStartAndEndTime(start, end))
            .isInstanceOf(BadRequestException.class);
    }
}
