package org.example.meetingscheduler.user;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.example.meetingscheduler.user.dto.UserRequestDto;
import org.example.meetingscheduler.user.dto.UserResponseDto;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public List<UserResponseDto> getUsers() {
        return this.userRepository.findAll().stream()
            .map(this.userMapper::toDto)
            .toList();
    }

    public UserResponseDto createUser(final UserRequestDto userRequestDto) {
        return this.userMapper.toDto(
            this.userRepository.save(
                UserEntity.builder()
                    .name(userRequestDto.name())
                    .email(userRequestDto.email())
                    .build()
            )
        );
    }
}
