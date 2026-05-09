package org.example.meetingscheduler.user;

import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserResponseDto toDto(final UserEntity userEntity);
}
