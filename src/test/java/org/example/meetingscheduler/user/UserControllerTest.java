package org.example.meetingscheduler.user;

import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

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

    @Test
    void getUsers_returnsEmptyArray_whenNoUsers() throws Exception {
        // Arrange
        when(this.userService.getUsers()).thenReturn(Collections.emptyList());

        // Act & Assert
        this.mockMvc.perform(get("/users"))
            .andExpect(status().isOk())
            .andExpect(content().json("[]"));
    }

    @Test
    void getUsers_returnsUsers() throws Exception {
        // Arrange
        when(this.userService.getUsers()).thenReturn(List.of(
            new UserResponseDto(1L, "Alice", "alice@example.com")));

        // Act & Assert
        this.mockMvc.perform(get("/users"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(1))
            .andExpect(jsonPath("$[0].name").value("Alice"))
            .andExpect(jsonPath("$[0].email").value("alice@example.com"));
    }

    @Test
    void createUser_returnsCreatedUser() throws Exception {
        // Arrange
        when(this.userService.createUser(new UserRequestDto("Alice", "alice@example.com")))
            .thenReturn(new UserResponseDto(1L, "Alice", "alice@example.com"));

        // Act & Assert
        this.mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Alice\",\"email\":\"alice@example.com\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.name").value("Alice"))
            .andExpect(jsonPath("$.email").value("alice@example.com"));
    }
}
