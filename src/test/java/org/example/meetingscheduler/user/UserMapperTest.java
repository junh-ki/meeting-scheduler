package org.example.meetingscheduler.user;

import org.example.meetingscheduler.user.dto.UserResponseDto;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserMapperTest {

    private final UserMapper userMapper = new UserMapperImpl();

    @Test
    void toDto_mapsAllFields() {
        // Arrange
        final UserEntity userEntity = UserEntity.builder()
            .id(1L)
            .name("Alice")
            .email("alice@example.com")
            .build();

        // Act
        final UserResponseDto userResponseDto = this.userMapper.toDto(userEntity);

        // Assert
        assertThat(userResponseDto.id()).isEqualTo(1L);
        assertThat(userResponseDto.name()).isEqualTo("Alice");
        assertThat(userResponseDto.email()).isEqualTo("alice@example.com");
    }

    @Test
    void toDto_nullEntity_mapsToNull() {
        // Arrange & Act & Assert
        assertThat(this.userMapper.toDto(null)).isNull();
    }
}
