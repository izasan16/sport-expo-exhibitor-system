package com.sportexpo.exhibitor.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportexpo.exhibitor.TestDatabaseSupport;
import com.sportexpo.exhibitor.auth.AuthErrorCodes;
import com.sportexpo.exhibitor.auth.AuthProperties;
import com.sportexpo.exhibitor.common.BusinessStatuses;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityIntegrationTest extends TestDatabaseSupport {
    private static final UUID ADMIN_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000201");
    private static final UUID DEMO_EXHIBITOR_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID OTHER_EVENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000991");
    private static final UUID OTHER_EXHIBITOR_ID = UUID.fromString("00000000-0000-0000-0000-000000000992");
    private static final UUID OTHER_AREA_TIER_ID = UUID.fromString("00000000-0000-0000-0000-000000000993");
    private static final UUID OTHER_ORDER_ID = UUID.fromString("00000000-0000-0000-0000-000000000994");
    private static final UUID OTHER_PAYMENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000995");
    private static final UUID OTHER_REGISTRATION_ID = UUID.fromString("00000000-0000-0000-0000-000000000996");
    private static final UUID OTHER_CONTRACT_ID = UUID.fromString("00000000-0000-0000-0000-000000000997");
    private static final UUID OTHER_CONTRACT_VERSION_ID = UUID.fromString("00000000-0000-0000-0000-000000000998");
    private static final UUID OTHER_SELECTION_ID = UUID.fromString("00000000-0000-0000-0000-000000000999");
    private static final UUID OTHER_ELIGIBILITY_ID = UUID.fromString("00000000-0000-0000-0000-000000000990");
    private static final UUID ASSIGNED_AREA_TIER_ID = UUID.fromString("00000000-0000-0000-0000-000000000981");
    private static final UUID ASSIGNED_ORDER_ID = UUID.fromString("00000000-0000-0000-0000-000000000982");
    private static final UUID ASSIGNED_PAYMENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000983");
    private static final UUID ASSIGNED_REGISTRATION_ID = UUID.fromString("00000000-0000-0000-0000-000000000984");
    private static final UUID ASSIGNED_CONTRACT_ID = UUID.fromString("00000000-0000-0000-0000-000000000985");
    private static final UUID ASSIGNED_CONTRACT_VERSION_ID = UUID.fromString("00000000-0000-0000-0000-000000000986");
    private static final UUID ASSIGNED_SELECTION_ID = UUID.fromString("00000000-0000-0000-0000-000000000987");

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    AuthProperties authProperties;
    @Autowired
    JdbcTemplate jdbcTemplate;
    @Value("${app.db.schema}")
    String schema;

    @Test
    void healthEndpointIsPublic() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk());
    }

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
    void lowercaseBearerSchemeIsAccepted() throws Exception {
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "bearer " + login("admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value("admin"));
    }

    @Test
    void authMeReturnsRicherCurrentUserPayloadForAdmin() throws Exception {
        var response = mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + login("admin")))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        var data = objectMapper.readTree(response).get("data");
        assertThat(data.get("userId").asText()).isEqualTo(ADMIN_USER_ID.toString());
        assertThat(data.get("username").asText()).isEqualTo("admin");
        assertThat(data.get("displayName")).as("displayName").isNotNull();
        assertThat(data.get("defaultRole")).as("defaultRole").isNotNull();
        assertThat(data.get("activeAvailableRoles")).as("activeAvailableRoles").isNotNull();
        assertThat(data.get("permissions")).as("permissions").isNotNull();
        assertThat(data.get("displayName").asText()).isEqualTo("系统管理员");
        assertThat(data.get("defaultRole").asText()).isEqualTo("SYSTEM_ADMIN");
        assertThat(stringValues(data.get("roles"))).containsExactly("SYSTEM_ADMIN");
        assertThat(stringValues(data.get("activeAvailableRoles"))).containsExactly("SYSTEM_ADMIN");
        assertThat(stringValues(data.get("permissions")))
                .contains("menu.admin.users", "page.admin.users.list");
    }

    @Test
    void logoutInvalidatesCurrentBearerToken() throws Exception {
        var token = login("admin");

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("admin"));

        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value(AuthErrorCodes.TOKEN_INVALID));
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

    @Test
    void salesAndFinanceCannotReadGlobalAdminDashboard() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard")
                        .header("Authorization", "Bearer " + login("sales01")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/admin/dashboard")
                        .header("Authorization", "Bearer " + login("finance01")))
                .andExpect(status().isForbidden());
    }

    @Test
    void exhibitorAndSalesCannotReadOtherExhibitorOrderDetail() throws Exception {
        seedOtherExhibitorGraph();

        mockMvc.perform(get("/api/participation-orders/{orderId}", OTHER_ORDER_ID)
                        .header("Authorization", "Bearer " + login("exhibitor01")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/participation-orders/{orderId}", OTHER_ORDER_ID)
                        .header("Authorization", "Bearer " + login("sales01")))
                .andExpect(status().isForbidden());
    }

    @Test
    void scopedListsDoNotLeakOtherExhibitorRecords() throws Exception {
        seedOtherExhibitorGraph();
        seedAssignedExhibitorGraph();
        var exhibitorToken = login("exhibitor01");
        var salesToken = login("sales01");

        assertListDoesNotContain(exhibitorToken, "/api/participation-orders", OTHER_ORDER_ID);
        assertListContains(salesToken, "/api/exhibitors", DEMO_EXHIBITOR_ID);
        assertListDoesNotContain(salesToken, "/api/exhibitors", OTHER_EXHIBITOR_ID);
        assertListDoesNotContain(exhibitorToken, "/api/registrations", OTHER_REGISTRATION_ID);
        assertListContains(salesToken, "/api/registrations", ASSIGNED_REGISTRATION_ID);
        assertListDoesNotContain(salesToken, "/api/registrations", OTHER_REGISTRATION_ID);
        assertListDoesNotContain(exhibitorToken, "/api/contracts", OTHER_CONTRACT_ID);
        assertListContains(salesToken, "/api/contracts", ASSIGNED_CONTRACT_ID);
        assertListDoesNotContain(salesToken, "/api/contracts", OTHER_CONTRACT_ID);
        assertListDoesNotContain(exhibitorToken, "/api/payments", OTHER_PAYMENT_ID);
        assertListContains(salesToken, "/api/payments", ASSIGNED_PAYMENT_ID);
        assertListDoesNotContain(salesToken, "/api/payments", OTHER_PAYMENT_ID);
        assertListDoesNotContain(exhibitorToken, "/api/booth-selections", OTHER_SELECTION_ID);
        assertListContains(salesToken, "/api/booth-selections", ASSIGNED_SELECTION_ID);
        assertListDoesNotContain(salesToken, "/api/booth-selections", OTHER_SELECTION_ID);
    }

    @Test
    void exhibitorCannotUpdateOrSubmitAnotherExhibitorRegistration() throws Exception {
        seedOtherExhibitorGraph();
        var exhibitorToken = login("exhibitor01");

        mockMvc.perform(post("/api/registrations/{id}/submit", OTHER_REGISTRATION_ID)
                        .header("Authorization", "Bearer " + exhibitorToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(
                                "/api/registrations/{id}", OTHER_REGISTRATION_ID)
                        .header("Authorization", "Bearer " + exhibitorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "eventId": "%s",
                                  "exhibitorId": "%s",
                                  "category": "SPORTS_EQUIPMENT",
                                  "contactName": "王老板",
                                  "contactPhone": "13800000000"
                                }
                                """.formatted(OTHER_EVENT_ID, DEMO_EXHIBITOR_ID)))
                .andExpect(status().isForbidden());
    }

    @Test
    void exhibitorCannotCreatePaymentForAnotherExhibitorOrder() throws Exception {
        seedOtherExhibitorGraph();

        mockMvc.perform(post("/api/payments")
                        .header("Authorization", "Bearer " + login("exhibitor01"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "orderId": "%s",
                                  "amount": 100.00,
                                  "method": "OFFLINE_TRANSFER"
                                }
                                """.formatted(OTHER_ORDER_ID)))
                .andExpect(status().isForbidden());
    }

    @Test
    void exhibitorCannotCreateEventOrBooth() throws Exception {
        seedOtherExhibitorGraph();
        var exhibitorToken = login("exhibitor01");

        mockMvc.perform(post("/api/events")
                        .header("Authorization", "Bearer " + exhibitorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Forbidden Event","year":2027,"status":"ACTIVE","defaultLockMinutes":1440}
                                """))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/booths")
                        .header("Authorization", "Bearer " + exhibitorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "eventId": "%s",
                                  "boothNo": "FORBIDDEN-A001",
                                  "areaSqm": 9.00,
                                  "boothType": "STANDARD",
                                  "status": "AVAILABLE"
                                }
                                """.formatted(OTHER_EVENT_ID)))
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
        var jwtVersion = jdbcTemplate.queryForObject("""
                select jwt_version
                from %s.users
                where id = ?
                """.formatted(schema), Integer.class, ADMIN_USER_ID);
        return Jwts.builder()
                .issuer(authProperties.getJwtIssuer())
                .subject(ADMIN_USER_ID.toString())
                .claim("username", "admin")
                .claim("roles", List.of("SYSTEM_ADMIN"))
                .claim("jwtVersion", jwtVersion)
                .id(UUID.randomUUID().toString())
                .issuedAt(Date.from(now.minusSeconds(120)))
                .expiration(Date.from(now.minusSeconds(60)))
                .signWith(Keys.hmacShaKeyFor(authProperties.signingKeyBytes()))
                .compact();
    }

    private void assertListDoesNotContain(String token, String url, UUID id) throws Exception {
        var response = mockMvc.perform(get(url)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        var data = objectMapper.readTree(response).get("data");
        assertThat(StreamSupport.stream(data.spliterator(), false)
                .map(node -> node.get("id").asText())
                .toList())
                .doesNotContain(id.toString());
    }

    private void assertListContains(String token, String url, UUID id) throws Exception {
        var response = mockMvc.perform(get(url)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        var data = objectMapper.readTree(response).get("data");
        assertThat(StreamSupport.stream(data.spliterator(), false)
                .map(node -> node.get("id").asText())
                .toList())
                .contains(id.toString());
    }

    private List<String> stringValues(JsonNode arrayNode) {
        return StreamSupport.stream(arrayNode.spliterator(), false)
                .map(JsonNode::asText)
                .toList();
    }

    private void seedOtherExhibitorGraph() {
        jdbcTemplate.update("""
                insert into %s.events(id, name, year, status, default_lock_minutes, created_at, updated_at)
                values (?, 'Other Event', 2026, 'ACTIVE', 1440, now(), now())
                on conflict (id) do nothing
                """.formatted(schema), OTHER_EVENT_ID);
        jdbcTemplate.update("""
                insert into %s.exhibitors(id, company_name, status, created_at, updated_at)
                values (?, 'Other Exhibitor', 'ACTIVE', now(), now())
                on conflict (id) do nothing
                """.formatted(schema), OTHER_EXHIBITOR_ID);
        jdbcTemplate.update("""
                insert into %s.area_tiers(id, event_id, area_sqm, enabled, created_at, updated_at)
                values (?, ?, 9.00, true, now(), now())
                on conflict do nothing
                """.formatted(schema), OTHER_AREA_TIER_ID, OTHER_EVENT_ID);
        jdbcTemplate.update("""
                insert into %s.participation_orders(
                    id, event_id, exhibitor_id, area_tier_id, area_sqm, booth_type, status,
                    area_fee, service_fee, discount_amount, adjustment_amount, total_amount,
                    created_at, updated_at, created_by, updated_by
                )
                values (?, ?, ?, ?, 9.00, 'STANDARD', ?, 10800.00, 0.00, 0.00, 0.00, 10800.00,
                        now(), now(), ?, ?)
                on conflict (id) do nothing
                """.formatted(schema), OTHER_ORDER_ID, OTHER_EVENT_ID, OTHER_EXHIBITOR_ID, OTHER_AREA_TIER_ID,
                BusinessStatuses.ORDER_PENDING_PAYMENT, OTHER_EXHIBITOR_ID, OTHER_EXHIBITOR_ID);
        jdbcTemplate.update("""
                insert into %s.registrations(
                    id, event_id, exhibitor_id, status, category, contact_name, contact_phone,
                    submitted_at, created_at, updated_at, created_by, updated_by
                )
                values (?, ?, ?, ?, 'SPORTS_EQUIPMENT', 'Other', '13900000000',
                        now(), now(), now(), ?, ?)
                on conflict (id) do nothing
                """.formatted(schema), OTHER_REGISTRATION_ID, OTHER_EVENT_ID, OTHER_EXHIBITOR_ID,
                BusinessStatuses.REGISTRATION_SUBMITTED, OTHER_EXHIBITOR_ID, OTHER_EXHIBITOR_ID);
        jdbcTemplate.update("""
                insert into %s.payments(
                    id, event_id, order_id, amount, status, method, created_at, updated_at, created_by, updated_by
                )
                values (?, ?, ?, 100.00, ?, 'OFFLINE_TRANSFER', now(), now(), ?, ?)
                on conflict (id) do nothing
                """.formatted(schema), OTHER_PAYMENT_ID, OTHER_EVENT_ID, OTHER_ORDER_ID,
                BusinessStatuses.PAYMENT_PENDING_FINANCE_CONFIRM, OTHER_EXHIBITOR_ID, OTHER_EXHIBITOR_ID);
        jdbcTemplate.update("""
                insert into %s.selection_eligibilities(
                    id, event_id, order_id, status, created_at, updated_at, created_by, updated_by
                )
                values (?, ?, ?, ?, now(), now(), ?, ?)
                on conflict (order_id) do nothing
                """.formatted(schema), OTHER_ELIGIBILITY_ID, OTHER_EVENT_ID, OTHER_ORDER_ID,
                BusinessStatuses.ELIGIBILITY_NOT_OPEN, OTHER_EXHIBITOR_ID, OTHER_EXHIBITOR_ID);
        jdbcTemplate.update("""
                insert into %s.contracts(
                    id, event_id, order_id, current_version_id, contract_no, status, created_at, updated_at,
                    created_by, updated_by
                )
                values (?, ?, ?, ?, 'OTHER-CONTRACT-001', ?, now(), now(), ?, ?)
                on conflict (id) do nothing
                """.formatted(schema), OTHER_CONTRACT_ID, OTHER_EVENT_ID, OTHER_ORDER_ID,
                OTHER_CONTRACT_VERSION_ID, BusinessStatuses.CONTRACT_GENERATED, OTHER_EXHIBITOR_ID, OTHER_EXHIBITOR_ID);
        jdbcTemplate.update("""
                insert into %s.contract_versions(
                    id, event_id, contract_id, version_no, reason, content_snapshot, amount, area_sqm,
                    sign_status, created_at, created_by
                )
                values (?, ?, ?, 1, 'security_test', '{}'::jsonb, 10800.00, 9.00, ?, now(), ?)
                on conflict (id) do nothing
                """.formatted(schema), OTHER_CONTRACT_VERSION_ID, OTHER_EVENT_ID, OTHER_CONTRACT_ID,
                BusinessStatuses.CONTRACT_VERSION_UNSIGNED, OTHER_EXHIBITOR_ID);
        jdbcTemplate.update("""
                insert into %s.booth_selections(
                    id, event_id, order_id, exhibitor_id, status, total_area_sqm,
                    created_at, updated_at, created_by, updated_by
                )
                values (?, ?, ?, ?, ?, 9.00, now(), now(), ?, ?)
                on conflict (id) do nothing
                """.formatted(schema), OTHER_SELECTION_ID, OTHER_EVENT_ID, OTHER_ORDER_ID, OTHER_EXHIBITOR_ID,
                BusinessStatuses.SELECTION_REJECTED, OTHER_EXHIBITOR_ID, OTHER_EXHIBITOR_ID);
    }

    private void seedAssignedExhibitorGraph() {
        jdbcTemplate.update("""
                insert into %s.area_tiers(id, event_id, area_sqm, enabled, created_at, updated_at)
                values (?, ?, 18.00, true, now(), now())
                on conflict do nothing
                """.formatted(schema), ASSIGNED_AREA_TIER_ID, OTHER_EVENT_ID);
        jdbcTemplate.update("""
                insert into %s.participation_orders(
                    id, event_id, exhibitor_id, area_tier_id, area_sqm, booth_type, status,
                    area_fee, service_fee, discount_amount, adjustment_amount, total_amount,
                    created_at, updated_at, created_by, updated_by
                )
                values (?, ?, ?, ?, 18.00, 'STANDARD', ?, 21600.00, 0.00, 0.00, 0.00, 21600.00,
                        now(), now(), ?, ?)
                on conflict (id) do nothing
                """.formatted(schema), ASSIGNED_ORDER_ID, OTHER_EVENT_ID, DEMO_EXHIBITOR_ID, ASSIGNED_AREA_TIER_ID,
                BusinessStatuses.ORDER_PENDING_PAYMENT, DEMO_EXHIBITOR_ID, DEMO_EXHIBITOR_ID);
        jdbcTemplate.update("""
                insert into %s.registrations(
                    id, event_id, exhibitor_id, status, category, contact_name, contact_phone,
                    submitted_at, reviewed_at, created_at, updated_at, created_by, updated_by
                )
                values (?, ?, ?, ?, 'SPORTS_EQUIPMENT', 'Assigned', '13800000000',
                        now(), now(), now(), now(), ?, ?)
                on conflict (id) do nothing
                """.formatted(schema), ASSIGNED_REGISTRATION_ID, OTHER_EVENT_ID, DEMO_EXHIBITOR_ID,
                BusinessStatuses.REGISTRATION_APPROVED, DEMO_EXHIBITOR_ID, DEMO_EXHIBITOR_ID);
        jdbcTemplate.update("""
                insert into %s.payments(
                    id, event_id, order_id, amount, status, method, created_at, updated_at, created_by, updated_by
                )
                values (?, ?, ?, 100.00, ?, 'OFFLINE_TRANSFER', now(), now(), ?, ?)
                on conflict (id) do nothing
                """.formatted(schema), ASSIGNED_PAYMENT_ID, OTHER_EVENT_ID, ASSIGNED_ORDER_ID,
                BusinessStatuses.PAYMENT_PENDING_FINANCE_CONFIRM, DEMO_EXHIBITOR_ID, DEMO_EXHIBITOR_ID);
        jdbcTemplate.update("""
                insert into %s.contracts(
                    id, event_id, order_id, current_version_id, contract_no, status, created_at, updated_at,
                    created_by, updated_by
                )
                values (?, ?, ?, ?, 'ASSIGNED-CONTRACT-001', ?, now(), now(), ?, ?)
                on conflict (id) do nothing
                """.formatted(schema), ASSIGNED_CONTRACT_ID, OTHER_EVENT_ID, ASSIGNED_ORDER_ID,
                ASSIGNED_CONTRACT_VERSION_ID, BusinessStatuses.CONTRACT_GENERATED, DEMO_EXHIBITOR_ID, DEMO_EXHIBITOR_ID);
        jdbcTemplate.update("""
                insert into %s.contract_versions(
                    id, event_id, contract_id, version_no, reason, content_snapshot, amount, area_sqm,
                    sign_status, created_at, created_by
                )
                values (?, ?, ?, 1, 'security_test', '{}'::jsonb, 21600.00, 18.00, ?, now(), ?)
                on conflict (id) do nothing
                """.formatted(schema), ASSIGNED_CONTRACT_VERSION_ID, OTHER_EVENT_ID, ASSIGNED_CONTRACT_ID,
                BusinessStatuses.CONTRACT_VERSION_UNSIGNED, DEMO_EXHIBITOR_ID);
        jdbcTemplate.update("""
                insert into %s.booth_selections(
                    id, event_id, order_id, exhibitor_id, status, total_area_sqm,
                    created_at, updated_at, created_by, updated_by
                )
                values (?, ?, ?, ?, ?, 18.00, now(), now(), ?, ?)
                on conflict (id) do nothing
                """.formatted(schema), ASSIGNED_SELECTION_ID, OTHER_EVENT_ID, ASSIGNED_ORDER_ID, DEMO_EXHIBITOR_ID,
                BusinessStatuses.SELECTION_REJECTED, DEMO_EXHIBITOR_ID, DEMO_EXHIBITOR_ID);
    }
}
