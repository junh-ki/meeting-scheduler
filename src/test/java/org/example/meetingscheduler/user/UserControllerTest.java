package org.example.meetingscheduler.user;

import java.util.Collections;
import java.util.List;
import org.example.meetingscheduler.user.dto.UserRequestDto;
import org.example.meetingscheduler.user.dto.UserResponseDto;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @MockitoBean
    private UserService userService;
    @Autowired
    private MockMvc mockMvc;

    @Nested
    class getUsers {

        @Test
        void returnsEmptyArray_whenNoUsers() throws Exception {
            // Arrange
            when(userService.getUsers()).thenReturn(Collections.emptyList());

            // Act & Assert
            mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
        }

        @Test
        void returnsUsers() throws Exception {
            // Arrange
            when(userService.getUsers()).thenReturn(List.of(
                new UserResponseDto(1L, "Alice", "alice@example.com")));

            // Act & Assert
            mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Alice"))
                .andExpect(jsonPath("$[0].email").value("alice@example.com"));
        }
    }

    @Nested
    class createUser {

        @Test
        void returnsConflict_whenEmailAlreadyExists() throws Exception {
            // Arrange
            when(userService.createUser(any(UserRequestDto.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Email is already in use"));

            // Act & Assert
            mockMvc.perform(post("/users")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"Alice\",\"email\":\"alice@example.com\"}"))
                .andExpect(status().isConflict());
        }

        @Test
        void returnsBadRequest_whenNameIsBlank() throws Exception {
            // Arrange & Act & Assert
            mockMvc.perform(post("/users")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"\",\"email\":\"alice@example.com\"}"))
                .andExpect(status().isBadRequest());
        }

        @Test
        void returnsBadRequest_whenEmailIsInvalid() throws Exception {
            // Arrange & Act & Assert
            mockMvc.perform(post("/users")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"Alice\",\"email\":\"not-an-email\"}"))
                .andExpect(status().isBadRequest());
        }

        @Test
        void returnsCreatedUser() throws Exception {
            // Arrange
            when(userService.createUser(new UserRequestDto("Alice", "alice@example.com")))
                .thenReturn(new UserResponseDto(1L, "Alice", "alice@example.com"));

            // Act & Assert
            mockMvc.perform(post("/users")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"Alice\",\"email\":\"alice@example.com\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Alice"))
                .andExpect(jsonPath("$.email").value("alice@example.com"));
        }
    }
}
