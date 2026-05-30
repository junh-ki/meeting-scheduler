package org.example.meetingscheduler.notification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.meetingscheduler.notification.dto.NotificationStatus;
import org.example.meetingscheduler.user.UserEntity;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "delete_notification")
public class DeleteNotificationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "meeting_id", nullable = false)
    private Long meetingId;

    @Column(name = "meeting_title", nullable = false)
    private String meetingTitle;

    @Column(name = "meeting_start_time", nullable = false)
    private LocalDateTime meetingStartTime;

    @Column(name = "meeting_end_time", nullable = false)
    private LocalDateTime meetingEndTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participant_id", nullable = false)
    private UserEntity participant;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationStatus status;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "last_attempt_at")
    private OffsetDateTime lastAttemptAt;

    @Column(name = "created_at", updatable = false, insertable = false)
    private OffsetDateTime createdAt;
}
