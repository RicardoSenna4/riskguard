package com.riskguard.transaction;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    @Test
    void registrationLoginRefreshAndRoleAuthorizationFlow() throws Exception {
        String registration = """
            {"email":"portfolio@example.com","password":"correct-horse-battery"}
            """;
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(registration))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.role").value("USER"));

        String login = """
            {"email":"PORTFOLIO@example.com","password":"correct-horse-battery"}
            """;
        String pairJson = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(login))
            .andExpect(status().isOk()).andExpect(jsonPath("$.accessToken").isNotEmpty())
            .andReturn().getResponse().getContentAsString();
        String token = mapper.readTree(pairJson).get("accessToken").asText();
        String refresh = mapper.readTree(pairJson).get("refreshToken").asText();

        mvc.perform(get("/api/v1/analyst/ping").header("Authorization", "Bearer " + token))
            .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/admin/ping")).andExpect(status().isUnauthorized());
        String rotatedPair = mvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(java.util.Map.of("refreshToken", refresh))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.refreshToken").isNotEmpty())
            .andReturn().getResponse().getContentAsString();
        String rotatedRefresh = mapper.readTree(rotatedPair).get("refreshToken").asText();
        String rotatedAccess = mapper.readTree(rotatedPair).get("accessToken").asText();

        mvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(java.util.Map.of("refreshToken", refresh))))
            .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/logout").header("Authorization", "Bearer " + rotatedAccess)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(java.util.Map.of("refreshToken", rotatedRefresh))))
            .andExpect(status().isOk());
        mvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(java.util.Map.of("refreshToken", rotatedRefresh))))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void userCanReadOwnProfileButNotAnotherUsersProfile() throws Exception {
        String first = """
            {"email":"owner@example.com","password":"correct-horse-battery"}
            """;
        String second = """
            {"email":"other@example.com","password":"correct-horse-battery"}
            """;
        String firstUser = mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(first))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String secondUser = mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(second))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String userId = mapper.readTree(firstUser).get("id").asText();
        String otherId = mapper.readTree(secondUser).get("id").asText();
        String login = "{\"email\":\"owner@example.com\",\"password\":\"correct-horse-battery\"}";
        String pair = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(login))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String accessToken = mapper.readTree(pair).get("accessToken").asText();

        mvc.perform(get("/api/v1/users/{userId}/profile", userId).header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk()).andExpect(jsonPath("$.email").value("owner@example.com"));
        mvc.perform(get("/api/v1/users/{userId}/profile", otherId).header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isForbidden());
    }

    @Test
    void duplicateRegistrationAndInvalidLoginAreRejected() throws Exception {
        String registration = """
            {"email":"duplicate@example.com","password":"correct-horse-battery"}
            """;
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(registration))
            .andExpect(status().isCreated());
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(registration))
            .andExpect(status().isConflict());
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
                    {"email":"duplicate@example.com","password":"wrong-password"}
                    """))
            .andExpect(status().isUnauthorized());
    }
}
