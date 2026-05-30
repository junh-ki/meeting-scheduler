package org.example.meetingscheduler;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.example.meetingscheduler.meeting.MeetingEntity;
import org.example.meetingscheduler.meeting.MeetingRepository;
import org.example.meetingscheduler.participant.ParticipantRepository;
import org.example.meetingscheduler.timeslot.SlotBookingStatus;
import org.example.meetingscheduler.timeslot.TimeslotEntity;
import org.example.meetingscheduler.timeslot.TimeslotRepository;
import org.example.meetingscheduler.user.UserEntity;
import org.example.meetingscheduler.user.UserRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests: full Spring context, real service business logic, repositories mocked.
 * Verifies the entire request-to-response cycle including GlobalExceptionHandler error bodies.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:integration;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
    "spring.flyway.enabled=false"
})
class MeetingSchedulerIntegrationTest {

    private static final LocalDateTime START = LocalDateTime.of(2026, 5, 10, 10, 0);
    private static final LocalDateTime END = LocalDateTime.of(2026, 5, 10, 11, 0);
    private static final UserEntity ALICE = UserEntity.builder()
        .id(1L).name("Alice").email("alice@example.com").build();
    private static final UserEntity BOB = UserEntity.builder()
        .id(2L).name("Bob").email("bob@example.com").build();
    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private UserRepository userRepository;
    @MockitoBean
    private TimeslotRepository timeslotRepository;
    @MockitoBean
    private MeetingRepository meetingRepository;
    @MockitoBean
    private ParticipantRepository participantRepository;

    @Nested
    class timeslots {

        @Test
        void createTimeslot_returnsCreated() throws Exception {
            // Arrange
            final TimeslotEntity saved = TimeslotEntity.builder()
                .id(10L)
                .owner(ALICE)
                .startTime(START)
                .endTime(END)
                .status(SlotBookingStatus.FREE)
                .build();
            when(userRepository.findByIdIs(1L)).thenReturn(Optional.of(ALICE));
            when(timeslotRepository.findAll(ArgumentMatchers.<Specification<TimeslotEntity>>any()))
                .thenReturn(List.of()) // coversRange: nothing covers the new range
                .thenReturn(List.of()); // overlapsOrAdjacent: no neighbors to merge
            when(timeslotRepository.save(any(TimeslotEntity.class))).thenReturn(saved);

            // Act & Assert
            mockMvc.perform(post("/users/1/timeslots")
                    .param("startTime", "2026-05-10T10:00:00")
                    .param("endTime", "2026-05-10T11:00:00"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.owner.name").value("Alice"))
                .andExpect(jsonPath("$.status").value("FREE"));
        }

        @Test
        void createTimeslot_returns404_whenUserDoesNotExist() throws Exception {
            // Arrange
            when(userRepository.findByIdIs(99L)).thenReturn(Optional.empty());

            // Act & Assert - service throws NotFoundException, handler produces {"status":404,"message":"User not found"}
            mockMvc.perform(post("/users/99/timeslots")
                    .param("startTime", "2026-05-10T10:00:00")
                    .param("endTime", "2026-05-10T11:00:00"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("User not found"));
        }

        @Test
        void createTimeslot_returns400_whenEndTimeEqualsStartTime() throws Exception {
            // TimeValidationUtil rejects before any repo is touched
            mockMvc.perform(post("/users/1/timeslots")
                    .param("startTime", "2026-05-10T10:00:00")
                    .param("endTime", "2026-05-10T10:00:00"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("endTime must be after startTime"));
        }

        @Test
        void createTimeslot_returns400_whenTimespansMidnight() throws Exception {
            mockMvc.perform(post("/users/1/timeslots")
                    .param("startTime", "2026-05-10T23:30:00")
                    .param("endTime", "2026-05-11T00:30:00"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("startTime and endTime must be on the same date"));
        }

        @Test
        void createTimeslot_returns409_whenRangeAlreadyCovered() throws Exception {
            // Arrange - existing 09:00-12:00 fully covers the new 10:00-11:00
            final TimeslotEntity covering = TimeslotEntity.builder()
                .id(1L)
                .owner(ALICE)
                .startTime(LocalDateTime.of(2026, 5, 10, 9, 0))
                .endTime(LocalDateTime.of(2026, 5, 10, 12, 0))
                .status(SlotBookingStatus.FREE)
                .build();
            when(userRepository.findByIdIs(1L)).thenReturn(Optional.of(ALICE));
            when(timeslotRepository.findAll(ArgumentMatchers.<Specification<TimeslotEntity>>any()))
                .thenReturn(List.of(covering));

            // Act & Assert
            mockMvc.perform(post("/users/1/timeslots")
                    .param("startTime", "2026-05-10T10:00:00")
                    .param("endTime", "2026-05-10T11:00:00"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("An existing timeslot already covers this time range"));
        }

        @Test
        void createTimeslot_returns409_whenOverlappingBookedSlot() throws Exception {
            // Arrange - new [09:00-12:00] overlaps existing BOOKED [10:00-11:00]
            final TimeslotEntity booked = TimeslotEntity.builder()
                .id(1L)
                .owner(ALICE)
                .startTime(LocalDateTime.of(2026, 5, 10, 10, 0))
                .endTime(LocalDateTime.of(2026, 5, 10, 11, 0))
                .status(SlotBookingStatus.BOOKED)
                .build();
            when(userRepository.findByIdIs(1L)).thenReturn(Optional.of(ALICE));
            when(timeslotRepository.findAll(ArgumentMatchers.<Specification<TimeslotEntity>>any()))
                .thenReturn(List.of())        // coversRange: no covering slot
                .thenReturn(List.of(booked)); // overlapsOrAdjacent: BOOKED slot overlaps

            // Act & Assert
            mockMvc.perform(post("/users/1/timeslots")
                    .param("startTime", "2026-05-10T09:00:00")
                    .param("endTime", "2026-05-10T12:00:00"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("New timeslot overlaps with a BOOKED timeslot"));
        }
    }

    @Nested
    class meetings {

        private static final String BODY_WITH_PARTICIPANT =
            "{\"title\":\"Sync\",\"startTime\":\"2026-05-10T10:00:00\",\"endTime\":\"2026-05-10T11:00:00\",\"participantUserIds\":[2]}";

        @Test
        void createMeeting_returns422_whenOrganizerHasNoFreeSlot() throws Exception {
            // Arrange - no covering FREE slot found for the organizer
            when(timeslotRepository.findAll(ArgumentMatchers.<Specification<TimeslotEntity>>any()))
                .thenReturn(List.of());

            // Act & Assert
            mockMvc.perform(post("/users/1/meetings")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(BODY_WITH_PARTICIPANT))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.message").value("No available timeslot for organizer"));
        }

        @Test
        void createMeeting_returns422_whenParticipantHasNoFreeSlot() throws Exception {
            // Arrange - organizer has a slot; participant does not
            final TimeslotEntity organizerSlot = TimeslotEntity.builder()
                .id(10L)
                .owner(ALICE)
                .startTime(START)
                .endTime(END)
                .status(SlotBookingStatus.FREE)
                .build();
            when(timeslotRepository.findAll(ArgumentMatchers.<Specification<TimeslotEntity>>any()))
                .thenReturn(List.of(organizerSlot))  // organizer covered
                .thenReturn(List.of());               // participant not covered

            // Act & Assert
            mockMvc.perform(post("/users/1/meetings")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(BODY_WITH_PARTICIPANT))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.message").value("No available timeslot for participant: 2"));
        }

        @Test
        void createMeeting_returns409_whenAlreadyExists() throws Exception {
            // Arrange
            final TimeslotEntity organizerSlot = TimeslotEntity.builder()
                .id(10L)
                .owner(ALICE)
                .startTime(START)
                .endTime(END)
                .status(SlotBookingStatus.FREE)
                .build();
            final TimeslotEntity participantSlot = TimeslotEntity.builder()
                .id(20L)
                .owner(BOB)
                .startTime(START)
                .endTime(END)
                .status(SlotBookingStatus.FREE)
                .build();
            when(timeslotRepository.findAll(ArgumentMatchers.<Specification<TimeslotEntity>>any()))
                .thenReturn(List.of(organizerSlot))
                .thenReturn(List.of(participantSlot));
            when(meetingRepository.existsByOrganizerIdAndStartTimeAndEndTime(1L, START, END))
                .thenReturn(true);

            // Act & Assert
            mockMvc.perform(post("/users/1/meetings")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(BODY_WITH_PARTICIPANT))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Meeting already exists for this time range"));
        }

        @Test
        void createMeeting_returns400_whenTitleIsBlank() throws Exception {
            // Bean Validation fires before service is reached
            mockMvc.perform(post("/users/1/meetings")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"title\":\"\",\"startTime\":\"2026-05-10T10:00:00\",\"endTime\":\"2026-05-10T11:00:00\",\"participantUserIds\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
        }

        @Test
        void createMeeting_returns400_whenEndTimeNotAfterStartTime() throws Exception {
            // Service's TimeValidationUtil fires and handler returns 400
            when(timeslotRepository.findAll(ArgumentMatchers.<Specification<TimeslotEntity>>any())).thenReturn(List.of());
            mockMvc.perform(post("/users/1/meetings")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"title\":\"Sync\",\"startTime\":\"2026-05-10T11:00:00\",\"endTime\":\"2026-05-10T10:00:00\",\"participantUserIds\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("endTime must be after startTime"));
        }

        @Test
        void deleteMeeting_returns404_whenMeetingDoesNotExist() throws Exception {
            // Arrange
            when(meetingRepository.findById(99L)).thenReturn(Optional.empty());

            // Act & Assert
            mockMvc.perform(delete("/users/1/meetings/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Meeting not found"));
        }

        @Test
        void deleteMeeting_returns403_whenCallerIsNotOrganizer() throws Exception {
            // Arrange - organizer is user 1, caller is user 2
            final MeetingEntity meeting = MeetingEntity.builder()
                .id(1L)
                .title("Sync")
                .startTime(START)
                .endTime(END)
                .organizer(ALICE)
                .participants(List.of())
                .build();
            when(meetingRepository.findById(1L)).thenReturn(Optional.of(meeting));

            // Act & Assert
            mockMvc.perform(delete("/users/2/meetings/1"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Meeting does not belong to this user"));
        }
    }
}
