package com.sportexpo.exhibitor.sales;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportexpo.exhibitor.TestDatabaseSupport;
import com.sportexpo.exhibitor.common.DemoActors;
import com.sportexpo.exhibitor.exhibitor.ExhibitorService;
import com.sportexpo.exhibitor.exhibitor.dto.ExhibitorRequest;
import com.sportexpo.exhibitor.security.SalesDataScopeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SalesAssignmentServiceTest extends TestDatabaseSupport {
    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    JdbcTemplate jdbcTemplate;
    @Autowired
    ExhibitorService exhibitorService;
    @Autowired
    SalesDataScopeService salesDataScopeService;
    @Value("${app.db.schema}")
    String schema;

    @Test
    void adminAssignsPrimaryAndCollaborators() throws Exception {
        var exhibitorId = createExhibitor("指派测试展商");
        var primarySalesUserId = insertSalesUser("sales-assignment-primary");
        var collaboratorSalesUserId = insertSalesUser("sales-assignment-collaborator");

        mockMvc.perform(put("/api/exhibitors/{id}/sales-assignments", exhibitorId)
                        .header("Authorization", "Bearer " + login("admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "primarySalesUserId": "%s",
                                  "collaboratorSalesUserIds": ["%s"]
                                }
                                """.formatted(primarySalesUserId, collaboratorSalesUserId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.exhibitorId").value(exhibitorId.toString()))
                .andExpect(jsonPath("$.data.primarySalesUserId").value(primarySalesUserId.toString()))
                .andExpect(jsonPath("$.data.collaboratorSalesUserIds[0]").value(collaboratorSalesUserId.toString()));

        assertThat(assignmentRows(exhibitorId))
                .containsExactlyInAnyOrder(
                        primarySalesUserId + ":PRIMARY",
                        collaboratorSalesUserId + ":COLLABORATOR"
                );

        mockMvc.perform(get("/api/exhibitors/{id}/sales-assignments", exhibitorId)
                        .header("Authorization", "Bearer " + login("admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.primarySalesUserId").value(primarySalesUserId.toString()))
                .andExpect(jsonPath("$.data.collaboratorSalesUserIds[0]").value(collaboratorSalesUserId.toString()));
    }

    @Test
    void cannotAssignTwoPrimarySales() throws Exception {
        var exhibitorId = createExhibitor("重复主销售测试展商");
        var primarySalesUserId = insertSalesUser("sales-assignment-duplicate");

        mockMvc.perform(put("/api/exhibitors/{id}/sales-assignments", exhibitorId)
                        .header("Authorization", "Bearer " + login("admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "primarySalesUserId": "%s",
                                  "collaboratorSalesUserIds": ["%s"]
                                }
                                """.formatted(primarySalesUserId, primarySalesUserId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("E_SALES_ASSIGNMENT_CONFLICT"));
    }

    @Test
    void cacheEvictsOldAndNewSalesAfterAssignmentChange() throws Exception {
        var exhibitorId = createExhibitor("缓存刷新测试展商");
        var oldPrimarySalesUserId = insertSalesUser("sales-assignment-old-primary");
        var oldCollaboratorSalesUserId = insertSalesUser("sales-assignment-old-collaborator");
        var newPrimarySalesUserId = insertSalesUser("sales-assignment-new-primary");
        var newCollaboratorSalesUserId = insertSalesUser("sales-assignment-new-collaborator");
        insertAssignment(exhibitorId, oldPrimarySalesUserId, "PRIMARY");
        insertAssignment(exhibitorId, oldCollaboratorSalesUserId, "COLLABORATOR");

        assertThat(salesDataScopeService.accessibleExhibitorIds(oldPrimarySalesUserId)).contains(exhibitorId);
        assertThat(salesDataScopeService.accessibleExhibitorIds(oldCollaboratorSalesUserId)).contains(exhibitorId);
        assertThat(salesDataScopeService.accessibleExhibitorIds(newPrimarySalesUserId)).doesNotContain(exhibitorId);
        assertThat(salesDataScopeService.accessibleExhibitorIds(newCollaboratorSalesUserId)).doesNotContain(exhibitorId);

        mockMvc.perform(put("/api/exhibitors/{id}/sales-assignments", exhibitorId)
                        .header("Authorization", "Bearer " + login("admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "primarySalesUserId": "%s",
                                  "collaboratorSalesUserIds": ["%s"]
                                }
                                """.formatted(newPrimarySalesUserId, newCollaboratorSalesUserId)))
                .andExpect(status().isOk());

        assertThat(salesDataScopeService.accessibleExhibitorIds(oldPrimarySalesUserId)).doesNotContain(exhibitorId);
        assertThat(salesDataScopeService.accessibleExhibitorIds(oldCollaboratorSalesUserId)).doesNotContain(exhibitorId);
        assertThat(salesDataScopeService.accessibleExhibitorIds(newPrimarySalesUserId)).contains(exhibitorId);
        assertThat(salesDataScopeService.accessibleExhibitorIds(newCollaboratorSalesUserId)).contains(exhibitorId);
    }

    @Test
    void salesCanOnlyAccessAssignedExhibitor() throws Exception {
        var otherExhibitorId = createExhibitor("未分配展商");

        mockMvc.perform(get("/api/exhibitors/{id}/sales-assignments", DemoActors.DEMO_EXHIBITOR_ID)
                        .header("Authorization", "Bearer " + login("sales01")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/exhibitors/{id}/sales-assignments", otherExhibitorId)
                        .header("Authorization", "Bearer " + login("sales01")))
                .andExpect(status().isForbidden());
    }

    private UUID createExhibitor(String companyName) {
        return exhibitorService.create(new ExhibitorRequest(
                companyName,
                "CREDIT-" + UUID.randomUUID(),
                "王经理",
                "13800000000",
                companyName + "@example.com",
                "上海市",
                "ACTIVE"
        ), DemoActors.DEMO_ADMIN_ID).id();
    }

    private UUID insertSalesUser(String usernamePrefix) {
        var id = UUID.randomUUID();
        var username = usernamePrefix + "-" + id.toString().substring(0, 8);
        jdbcTemplate.update("""
                insert into %s.users(
                    id, username, password_hash, display_name, phone, email, user_type, exhibitor_id, status,
                    default_role, failed_login_count, jwt_version, created_at, updated_at, created_by, updated_by
                )
                values (?, ?, ?, ?, null, ?, 'STAFF', null, 'ACTIVE', 'SALES', 0, 0, now(), now(), ?, ?)
                """.formatted(schema),
                id,
                username,
                "$2b$10$Xq2VGIvlkBrDmwGUNPhgaut9mofpvTOMkBuuTHzHBP01vEQ0/a2HG",
                "测试销售-" + username,
                username + "@example.com",
                DemoActors.DEMO_ADMIN_ID,
                DemoActors.DEMO_ADMIN_ID);
        jdbcTemplate.update("""
                insert into %s.user_roles(user_id, role_id, created_at)
                select ?, id, now()
                from %s.roles
                where code = 'SALES'
                """.formatted(schema, schema), id);
        return id;
    }

    private void insertAssignment(UUID exhibitorId, UUID salesUserId, String assignmentType) {
        jdbcTemplate.update("""
                insert into %s.exhibitor_sales_assignments(
                    id, exhibitor_id, sales_user_id, assignment_type, created_at, updated_at, created_by, updated_by
                )
                values (?, ?, ?, ?, now(), now(), ?, ?)
                """.formatted(schema),
                UUID.randomUUID(),
                exhibitorId,
                salesUserId,
                assignmentType,
                DemoActors.DEMO_ADMIN_ID,
                DemoActors.DEMO_ADMIN_ID);
    }

    private List<String> assignmentRows(UUID exhibitorId) {
        return jdbcTemplate.query("""
                        select sales_user_id, assignment_type
                        from %s.exhibitor_sales_assignments
                        where exhibitor_id = ?
                        """.formatted(schema),
                (rs, rowNum) -> rs.getObject("sales_user_id", UUID.class) + ":" + rs.getString("assignment_type"),
                exhibitorId);
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
}
