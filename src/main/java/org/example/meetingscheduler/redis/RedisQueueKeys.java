package org.example.meetingscheduler.redis;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RedisQueueKeys {

    public static final String SCHEDULE_QUEUE = "queue:notification:scheduled";
    public static final String DELETE_QUEUE = "queue:notification:deleted";
}
