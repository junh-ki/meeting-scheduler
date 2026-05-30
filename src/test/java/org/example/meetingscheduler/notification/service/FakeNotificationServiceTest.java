package org.example.meetingscheduler.notification.service;

import feign.FeignException;
import java.time.LocalDateTime;
import org.example.meetingscheduler.notification.dto.NotificationEventType;
import org.example.meetingscheduler.notification.dto.NotificationRequestDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FakeNotificationServiceTest {

    @InjectMocks
    private FakeNotificationService fakeNotificationService;
    @Mock
    private NotificationFeignClient notificationFeignClient;

    @Test
    void notify_delegatesToFeignClient() {
        // Arrange
        final NotificationRequestDto request = sampleRequest();

        // Act
        this.fakeNotificationService.notify(request);

        // Assert
        verify(this.notificationFeignClient).notify(request);
    }

    @Test
    void notify_propagatesExceptionFromFeignClient() {
        // Arrange
        final NotificationRequestDto request = sampleRequest();
        final FeignException feignException = mock(FeignException.class);
        doThrow(feignException).when(this.notificationFeignClient).notify(request);

        // Act & Assert
        // @Retry AOP does not apply in a plain Mockito unit test — exception propagates directly.
        // Retry behavior is covered by Resilience4j's own test suite and integration tests.
        assertThatThrownBy(() -> this.fakeNotificationService.notify(request))
            .isInstanceOf(FeignException.class);
    }

    private NotificationRequestDto sampleRequest() {
        return new NotificationRequestDto(
            1L, "Sync",
            LocalDateTime.of(2026, 5, 10, 10, 0),
            LocalDateTime.of(2026, 5, 10, 11, 0),
            NotificationEventType.MEETING_CREATED,
            2L, "Bob", "bob@example.com"
        );
    }
}
