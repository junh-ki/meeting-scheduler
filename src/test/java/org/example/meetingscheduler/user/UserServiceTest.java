package org.example.meetingscheduler.user;

import java.util.Collections;
import java.util.List;
import org.example.meetingscheduler.user.dto.UserRequestDto;
import org.example.meetingscheduler.user.dto.UserResponseDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

    @Test
    void getUsers_returnsEmptyList_whenRepositoryIsEmpty() {
        // Arrange
        when(this.userRepository.findAll()).thenReturn(Collections.emptyList());

        // Act & Assert
        assertThat(this.userService.getUsers()).isEmpty();
    }

    @Test
    void getUsers_returnsMappedDtos() {
        // Arrange
        final UserEntity userEntity = UserEntity.builder()
            .id(1L)
            .name("Alice")
            .email("alice@example.com")
            .build();
        final UserResponseDto userResponseDto = new UserResponseDto(1L, "Alice", "alice@example.com");
        when(this.userRepository.findAll()).thenReturn(List.of(userEntity));
        when(this.userMapper.toDto(userEntity)).thenReturn(userResponseDto);

        // Act & Assert
        assertThat(this.userService.getUsers()).containsExactly(userResponseDto);
    }

    @Test
    void createUser_savesAndReturnsMappedDto() {
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
        when(this.userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(this.userRepository.save(unsavedEntity)).thenReturn(savedEntity);
        when(this.userMapper.toDto(savedEntity)).thenReturn(userResponseDto);

        // Act & Assert
        assertThat(this.userService.createUser(userRequestDto)).isEqualTo(userResponseDto);
    }

    @Test
    void createUser_throwsConflict_whenEmailAlreadyExists() {
        // Arrange
        when(this.userRepository.existsByEmail("alice@example.com")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> this.userService.createUser(new UserRequestDto("Alice", "alice@example.com")))
            .isInstanceOf(ResponseStatusException.class)
            .extracting(e -> ((ResponseStatusException) e).getStatusCode())
            .isEqualTo(HttpStatus.CONFLICT);
    }
}
