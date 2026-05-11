package org.example.meetingscheduler.user;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.meetingscheduler.user.dto.UserRequestDto;
import org.example.meetingscheduler.user.dto.UserResponseDto;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
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
        if (this.userRepository.existsByEmail(userRequestDto.email())) {
            log.warn("Duplicate user creation rejected: email={}", userRequestDto.email());
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already in use");
        }
        try {
            return this.userMapper.toDto(
                this.userRepository.save(
                    UserEntity.builder()
                        .name(userRequestDto.name())
                        .email(userRequestDto.email())
                        .build()
                )
            );
        } catch (final DataIntegrityViolationException dataIntegrityViolationException) {
            log.warn(
                "Concurrent duplicate user creation rejected: email={}",
                userRequestDto.email()
            );
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Email is already in use"
            );
        }
    }
}
