package org.example.meetingscheduler.user;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

    boolean existsByEmail(final String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<UserEntity> findByIdIs(final Long id);
}
