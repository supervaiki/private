package sn.unchk.librarymanagement.controller.author;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import sn.unchk.librarymanagement.config.TokenPropertiesTests;
import sn.unchk.librarymanagement.controller.ControllerBaseTest;
import sn.unchk.librarymanagement.domain.exceptions.AlreadyExistsException;
import sn.unchk.librarymanagement.domain.exceptions.MalformedFieldException;
import sn.unchk.librarymanagement.domain.exceptions.NotFoundException;
import sn.unchk.librarymanagement.domain.models.book.Author;
import sn.unchk.librarymanagement.presentation.dto.reponse.AuthorResponse;
import sn.unchk.librarymanagement.presentation.dto.request.AuthorRequest;
import sn.unchk.librarymanagement.service.author.AuthorService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Rollback
class AuthorControllerImplTest extends ControllerBaseTest {

    @MockBean
    private AuthorService authorService;

    private AuthorRequest validAuthorRequest;
    private AuthorResponse authorResponse;
    private UUID authorId;

    public AuthorControllerImplTest(ObjectMapper objectMapper, MockMvc mockMvc, TokenPropertiesTests tokenPropertiesTests) {
        super(objectMapper, mockMvc, tokenPropertiesTests);
    }

    @BeforeEach
    void setUp() {
        authorId = UUID.randomUUID();
        
        validAuthorRequest = AuthorRequest.builder()
                .name("Chinua Achebe")
                .dateOfBirth(LocalDate.of(1930, 11, 16))
                .biography("Nigerian novelist and poet")
                .build();

        authorResponse = new AuthorResponse(
                authorId,
                "Chinua Achebe",
                LocalDate.of(1930, 11, 16),
                "Nigerian novelist and poet",
                LocalDateTime.now(),
                LocalDateTime.now(),
                "system",
                "system"
        );
    }

    @Test
    @WithMockUser(authorities = {"ADMIN"})
    void addAuthor_WithValidRequest_ShouldReturnSuccess() throws Exception {
        // Given
        Author mockAuthor = Author.builder()
                .id(authorId)
                .name(validAuthorRequest.getName())
                .dateOfBirth(validAuthorRequest.getDateOfBirth())
                .biography(validAuthorRequest.getBiography())
                .build();
        
        when(authorService.addAuthor(any(AuthorRequest.class))).thenReturn(mockAuthor);

        // When & Then
        mockMvc.perform(post("/authors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(convert(validAuthorRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Author is created successfully"))
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(authorities = {"READER"})
    void addAuthor_WithReaderRole_ShouldReturnForbidden() throws Exception {
        // When & Then
        mockMvc.perform(post("/authors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(convert(validAuthorRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = {"ADMIN"})
    void addAuthor_WithDuplicateName_ShouldReturnConflict() throws Exception {
        // Given
        when(authorService.addAuthor(any(AuthorRequest.class)))
                .thenThrow(new AlreadyExistsException("Author"));

        // When & Then
        mockMvc.perform(post("/authors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(convert(validAuthorRequest)))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(authorities = {"ADMIN"})
    void addAuthor_WithInvalidRequest_ShouldReturnBadRequest() throws Exception {
        // Given
        AuthorRequest invalidRequest = AuthorRequest.builder()
                .name(null) // Invalid: name is required for Create group
                .dateOfBirth(LocalDate.of(1930, 11, 16))
                .biography("Test biography")
                .build();

        // When & Then
        mockMvc.perform(post("/authors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(convert(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = {"ADMIN"})
    void updateAuthor_WithValidRequest_ShouldReturnSuccess() throws Exception {
        // Given
        Author mockAuthor = Author.builder().id(authorId).build();
        when(authorService.updateAuthor(eq(authorId), any(AuthorRequest.class))).thenReturn(mockAuthor);

        // When & Then
        mockMvc.perform(patch("/authors/{id}", authorId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(convert(validAuthorRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Author is updated successfully"))
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(authorities = {"ADMIN"})
    void updateAuthor_WithNonExistentId_ShouldReturnNotFound() throws Exception {
        // Given
        when(authorService.updateAuthor(eq(authorId), any(AuthorRequest.class)))
                .thenThrow(new NotFoundException("id", "Author with id " + authorId + " not found"));

        // When & Then
        mockMvc.perform(patch("/authors/{id}", authorId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(convert(validAuthorRequest)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(authorities = {"ADMIN"})
    void deleteAuthor_WithValidId_ShouldReturnSuccess() throws Exception {
        // When & Then
        mockMvc.perform(delete("/authors/{id}", authorId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Author deleted successfully"))
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(authorities = {"ADMIN"})
    void deleteAuthor_WithLinkedBooks_ShouldReturnBadRequest() throws Exception {
        // Given
        doThrow(new MalformedFieldException("id", "Cannot delete author with linked books"))
                .when(authorService).deleteAuthor(authorId);

        // When & Then
        mockMvc.perform(delete("/authors/{id}", authorId))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = {"READER"})
    void deleteAuthor_WithReaderRole_ShouldReturnForbidden() throws Exception {
        // When & Then
        mockMvc.perform(delete("/authors/{id}", authorId))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = {"READER"})
    void getAllAuthors_WithReaderRole_ShouldReturnAuthors() throws Exception {
        // Given
        when(authorService.retrieveAll()).thenReturn(List.of(authorResponse));

        // When & Then
        mockMvc.perform(get("/authors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].name").value("Chinua Achebe"));
    }

    @Test
    @WithMockUser(authorities = {"ADMIN"})
    void getAllAuthors_WithAdminRole_ShouldReturnAuthors() throws Exception {
        // Given
        when(authorService.retrieveAll()).thenReturn(List.of(authorResponse));

        // When & Then
        mockMvc.perform(get("/authors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].name").value("Chinua Achebe"));
    }

    @Test
    @WithMockUser(authorities = {"READER"})
    void getAuthorInfo_WithValidId_ShouldReturnAuthor() throws Exception {
        // Given
        when(authorService.retrieveAuthorInfo(authorId)).thenReturn(authorResponse);

        // When & Then
        mockMvc.perform(get("/authors/{id}", authorId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Chinua Achebe"))
                .andExpect(jsonPath("$.id").value(authorId.toString()));
    }

    @Test
    @WithMockUser(authorities = {"ADMIN"})
    void getAuthorInfo_WithNonExistentId_ShouldReturnNotFound() throws Exception {
        // Given
        when(authorService.retrieveAuthorInfo(authorId))
                .thenThrow(new NotFoundException("id", "Author with id " + authorId + " not found"));

        // When & Then
        mockMvc.perform(get("/authors/{id}", authorId))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAllAuthors_WithoutAuthentication_ShouldReturnUnauthorized() throws Exception {
        // When & Then
        mockMvc.perform(get("/authors"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void addAuthor_WithoutAuthentication_ShouldReturnUnauthorized() throws Exception {
        // When & Then
        mockMvc.perform(post("/authors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(convert(validAuthorRequest)))
                .andExpect(status().isUnauthorized());
    }
}