package com.sportexpo.exhibitor.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportexpo.exhibitor.TestDatabaseSupport;
import com.sportexpo.exhibitor.auth.AuthErrorCodes;
import com.sportexpo.exhibitor.auth.AuthProperties;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityIntegrationTest extends TestDatabaseSupport {
    private static final UUID ADMIN_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000201");

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    AuthProperties authProperties;

    @Test
    void businessApiWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/exhibitors/current"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tamperedTokenReturns401() throws Exception {
        var token = login("admin");
        var tamperedToken = token.substring(0, token.length() - 1) + "x";

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + tamperedToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value(AuthErrorCodes.TOKEN_INVALID));
    }

    @Test
    void expiredTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + expiredAdminToken()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value(AuthErrorCodes.TOKEN_EXPIRED));
    }

    @Test
    void roleMismatchReturns403() throws Exception {
        var exhibitorToken = login("exhibitor01");
        var requestBody = """
                {
                  "eventId": "00000000-0000-0000-0000-000000000901",
                  "code": "SECURITY_TEST",
                  "name": "Security Test",
                  "enabled": true,
                  "sortOrder": 99
                }
                """;

        mockMvc.perform(post("/api/event-categories")
                        .header("Authorization", "Bearer " + exhibitorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isForbidden());
    }

    private String login(String username) throws Exception {
        var response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"demo123"}
                                """.formatted(username)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode body = objectMapper.readTree(response);
        return body.get("data").get("accessToken").asText();
    }

    private String expiredAdminToken() {
        var now = Instant.now();
        return Jwts.builder()
                .issuer(authProperties.getJwtIssuer())
                .subject(ADMIN_USER_ID.toString())
                .claim("username", "admin")
                .claim("roles", List.of("SYSTEM_ADMIN"))
                .id(UUID.randomUUID().toString())
                .issuedAt(Date.from(now.minusSeconds(120)))
                .expiration(Date.from(now.minusSeconds(60)))
                .signWith(Keys.hmacShaKeyFor(authProperties.signingKeyBytes()))
                .compact();
    }
}
