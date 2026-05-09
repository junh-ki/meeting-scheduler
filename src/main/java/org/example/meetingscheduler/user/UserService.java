package org.example.meetingscheduler.user;

import java.util.List;
import lombok.RequiredArgsConstructor;
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
}
