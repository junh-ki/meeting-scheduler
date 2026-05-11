package org.example.meetingscheduler.user;

import java.util.Collections;
import java.util.List;
import org.example.meetingscheduler.user.dto.UserRequestDto;
import org.example.meetingscheduler.user.dto.UserResponseDto;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @InjectMocks
    private UserService userService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserMapper userMapper;

    @Nested
    class getUsers {

        @Test
        void returnsEmptyList_whenRepositoryIsEmpty() {
            // Arrange
            when(userRepository.findAll()).thenReturn(Collections.emptyList());

            // Act & Assert
            assertThat(userService.getUsers()).isEmpty();
        }

        @Test
        void returnsMappedDtos() {
            // Arrange
            final UserEntity userEntity = UserEntity.builder()
                .id(1L)
                .name("Alice")
                .email("alice@example.com")
                .build();
            final UserResponseDto userResponseDto = new UserResponseDto(1L, "Alice", "alice@example.com");
            when(userRepository.findAll()).thenReturn(List.of(userEntity));
            when(userMapper.toDto(userEntity)).thenReturn(userResponseDto);

            // Act & Assert
            assertThat(userService.getUsers()).containsExactly(userResponseDto);
        }
    }

    @Nested
    class createUser {

        @Test
        void savesAndReturnsMappedDto() {
            // Arrange
            final UserRequestDto userRequestDto = new UserRequestDto("Alice", "alice@example.com");
            final UserEntity savedEntity = UserEntity.builder()
                .id(1L)
                .name("Alice")
                .email("alice@example.com")
                .build();
            final UserResponseDto userResponseDto = new UserResponseDto(1L, "Alice", "alice@example.com");
            final UserEntity unsavedEntity = UserEntity.builder()
                .name("Alice")
                .email("alice@example.com")
                .build();
            when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
            when(userRepository.save(unsavedEntity)).thenReturn(savedEntity);
            when(userMapper.toDto(savedEntity)).thenReturn(userResponseDto);

            // Act & Assert
            assertThat(userService.createUser(userRequestDto)).isEqualTo(userResponseDto);
        }

        @Test
        void throwsConflict_whenEmailAlreadyExists() {
            // Arrange
            when(userRepository.existsByEmail("alice@example.com")).thenReturn(true);

            // Act & Assert
            assertThatThrownBy(() -> userService.createUser(new UserRequestDto("Alice", "alice@example.com")))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
        }

        @Test
        void throwsConflict_whenConcurrentInsertViolatesUniqueConstraint() {
            // Arrange: existsByEmail passes the check but save races and hits the DB constraint
            final UserEntity unsavedUserEntity = UserEntity.builder()
                .name("Alice").email("alice@example.com").build();
            when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
            when(userRepository.save(unsavedUserEntity)).thenThrow(new DataIntegrityViolationException("unique constraint"));

            // Act & Assert
            assertThatThrownBy(() -> userService.createUser(new UserRequestDto("Alice", "alice@example.com")))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
        }
    }
}
