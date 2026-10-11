package com.vocably.auth;

import com.jayway.jsonpath.JsonPath;
import com.vocably.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AuthControllerTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    UserRepository users;

    @BeforeEach
    void cleanDatabase() {
        users.deleteAll();
    }

    @Test
    void registerStoresHashedPasswordAndReturnsToken() throws Exception {
        register("Alice@Example.com", "supersecret")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.displayName").value("Alice"));

        User saved = users.findByEmail("alice@example.com").orElseThrow();
        assertThat(saved.getPasswordHash()).isNotEqualTo("supersecret").startsWith("$2");
    }

    @Test
    void rejectsDuplicateEmail() throws Exception {
        register("alice@example.com", "supersecret").andExpect(status().isCreated());

        register("ALICE@example.com", "anotherpass")
                .andExpect(status().isConflict());
    }

    @Test
    void rejectsShortPassword() throws Exception {
        register("alice@example.com", "123").andExpect(status().isBadRequest());
    }

    @Test
    void loginReturnsTokenThatAuthenticatesRequests() throws Exception {
        register("alice@example.com", "supersecret").andExpect(status().isCreated());

        String body = login("alice@example.com", "supersecret")
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(body, "$.token");

        // Fluxo completo: o token gerado no login é aceito numa rota protegida.
        mvc.perform(get("/api/words").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void loginFailsWithWrongPassword() throws Exception {
        register("alice@example.com", "supersecret").andExpect(status().isCreated());

        login("alice@example.com", "wrongpassword")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Invalid email or password"));
    }

    @Test
    void rejectsInvalidToken() throws Exception {
        mvc.perform(get("/api/words").header("Authorization", "Bearer not-a-real-token"))
                .andExpect(status().isUnauthorized());
    }

    private ResultActions register(String email, String password) throws Exception {
        return mvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email": "%s", "password": "%s", "displayName": "Alice"}
                        """.formatted(email, password)));
    }

    private ResultActions login(String email, String password) throws Exception {
        return mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email": "%s", "password": "%s"}
                        """.formatted(email, password)));
    }
}
