package com.sportexpo.exhibitor.sales;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportexpo.exhibitor.TestDatabaseSupport;
import com.sportexpo.exhibitor.common.DemoActors;
import com.sportexpo.exhibitor.exhibitor.ExhibitorService;
import com.sportexpo.exhibitor.exhibitor.dto.ExhibitorRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SalesFollowUpServiceTest extends TestDatabaseSupport {
    private static final UUID SALES01_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000203");

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    JdbcTemplate jdbcTemplate;
    @Autowired
    ExhibitorService exhibitorService;
    @Value("${app.db.schema}")
    String schema;

    @Test
    void salesCanCreateOnlyForAccessibleExhibitor() throws Exception {
        var otherExhibitorId = createExhibitor("跟进未授权展商");
        var salesToken = login("sales01");

        mockMvc.perform(post("/api/sales-follow-ups")
                        .header("Authorization", "Bearer " + salesToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "exhibitorId": "%s",
                                  "method": "PHONE",
                                  "content": "已联系展商确认搭建需求",
                                  "status": "OPEN"
                                }
                                """.formatted(DemoActors.DEMO_EXHIBITOR_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.exhibitorId").value(DemoActors.DEMO_EXHIBITOR_ID.toString()))
                .andExpect(jsonPath("$.data.salesUserId").value(SALES01_USER_ID.toString()))
                .andExpect(jsonPath("$.data.status").value("OPEN"));

        mockMvc.perform(post("/api/sales-follow-ups")
                        .header("Authorization", "Bearer " + salesToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "exhibitorId": "%s",
                                  "method": "PHONE",
                                  "content": "越权测试",
                                  "status": "OPEN"
                                }
                                """.formatted(otherExhibitorId)))
                .andExpect(status().isForbidden());
    }

    @Test
    void salesCanEditOnlyOwnFollowUps() throws Exception {
        var otherSalesUserId = insertSalesUser("follow-up-other-sales");
        insertAssignment(DemoActors.DEMO_EXHIBITOR_ID, otherSalesUserId, "COLLABORATOR");
        var otherFollowUpId = insertFollowUp(DemoActors.DEMO_EXHIBITOR_ID, otherSalesUserId, "PHONE", "其他销售记录", "OPEN");
        var ownFollowUpId = insertFollowUp(DemoActors.DEMO_EXHIBITOR_ID, SALES01_USER_ID, "PHONE", "自己的跟进记录", "OPEN");
        var salesToken = login("sales01");

        mockMvc.perform(put("/api/sales-follow-ups/{id}", otherFollowUpId)
                        .header("Authorization", "Bearer " + salesToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "exhibitorId": "%s",
                                  "method": "VISIT",
                                  "content": "尝试修改他人记录",
                                  "status": "DONE"
                                }
                                """.formatted(DemoActors.DEMO_EXHIBITOR_ID)))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/sales-follow-ups/{id}", ownFollowUpId)
                        .header("Authorization", "Bearer " + salesToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "exhibitorId": "%s",
                                  "method": "VISIT",
                                  "content": "已到场拜访并确认合同细节",
                                  "status": "DONE"
                                }
                                """.formatted(DemoActors.DEMO_EXHIBITOR_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(ownFollowUpId.toString()))
                .andExpect(jsonPath("$.data.content").value("已到场拜访并确认合同细节"))
                .andExpect(jsonPath("$.data.status").value("DONE"));

        assertThat(followUpContent(ownFollowUpId)).isEqualTo("已到场拜访并确认合同细节");
    }

    @Test
    void adminCanListAllFollowUps() throws Exception {
        var otherExhibitorId = createExhibitor("管理员查看全部跟进");
        var otherSalesUserId = insertSalesUser("follow-up-admin-list");
        insertAssignment(otherExhibitorId, otherSalesUserId, "PRIMARY");
        var demoFollowUpId = insertFollowUp(DemoActors.DEMO_EXHIBITOR_ID, SALES01_USER_ID, "PHONE", "演示展商跟进", "OPEN");
        var otherFollowUpId = insertFollowUp(otherExhibitorId, otherSalesUserId, "WECHAT", "其他展商跟进", "DONE");

        var response = mockMvc.perform(get("/api/sales-follow-ups")
                        .header("Authorization", "Bearer " + login("admin")))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        var ids = StreamSupport.stream(objectMapper.readTree(response).get("data").spliterator(), false)
                .map(node -> UUID.fromString(node.get("id").asText()))
                .toList();
        assertThat(ids).contains(demoFollowUpId, otherFollowUpId);
    }

    @Test
    void exhibitorCannotAccessFollowUps() throws Exception {
        mockMvc.perform(get("/api/sales-follow-ups")
                        .header("Authorization", "Bearer " + login("exhibitor01")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminStatusUpdateDoesNotOverwriteOriginalContent() throws Exception {
        var followUpId = insertFollowUp(DemoActors.DEMO_EXHIBITOR_ID, SALES01_USER_ID, "PHONE", "初始跟进内容", "OPEN");

        mockMvc.perform(put("/api/sales-follow-ups/{id}", followUpId)
                        .header("Authorization", "Bearer " + login("admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "exhibitorId": "%s",
                                  "method": "VISIT",
                                  "content": "管理员不应覆盖这段内容",
                                  "status": "DONE"
                                }
                                """.formatted(DemoActors.DEMO_EXHIBITOR_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DONE"))
                .andExpect(jsonPath("$.data.content").value("初始跟进内容"));

        assertThat(followUpContent(followUpId)).isEqualTo("初始跟进内容");
        assertThat(followUpStatus(followUpId)).isEqualTo("DONE");
    }

    private UUID createExhibitor(String companyName) {
        return exhibitorService.create(new ExhibitorRequest(
                companyName,
                "CREDIT-" + UUID.randomUUID(),
                "李经理",
                "13900000000",
                companyName + "@example.com",
                "北京市",
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

    private UUID insertFollowUp(UUID exhibitorId, UUID salesUserId, String method, String content, String statusValue) {
        var id = UUID.randomUUID();
        jdbcTemplate.update("""
                insert into %s.sales_follow_ups(
                    id, exhibitor_id, sales_user_id, method, content, next_follow_up_at, status,
                    created_at, updated_at, created_by, updated_by
                )
                values (?, ?, ?, ?, ?, ?, ?, now(), now(), ?, ?)
                """.formatted(schema),
                id,
                exhibitorId,
                salesUserId,
                method,
                content,
                OffsetDateTime.now().plusDays(3),
                statusValue,
                salesUserId,
                salesUserId);
        return id;
    }

    private String followUpContent(UUID followUpId) {
        return jdbcTemplate.queryForObject("""
                select content
                from %s.sales_follow_ups
                where id = ?
                """.formatted(schema), String.class, followUpId);
    }

    private String followUpStatus(UUID followUpId) {
        return jdbcTemplate.queryForObject("""
                select status
                from %s.sales_follow_ups
                where id = ?
                """.formatted(schema), String.class, followUpId);
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
