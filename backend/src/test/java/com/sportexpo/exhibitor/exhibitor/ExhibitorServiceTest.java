package com.sportexpo.exhibitor.exhibitor;

import com.sportexpo.exhibitor.TestDatabaseSupport;
import com.sportexpo.exhibitor.common.DemoActors;
import com.sportexpo.exhibitor.exhibitor.dto.ExhibitorRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ExhibitorServiceTest extends TestDatabaseSupport {
    @Autowired
    ExhibitorService exhibitorService;

    @Test
    void createsAndUpdatesExhibitorProfile() {
        var created = exhibitorService.create(new ExhibitorRequest(
                "演示展商",
                "CREDIT-" + UUID.randomUUID(),
                "王老板",
                "13800000000",
                "demo@example.com",
                "上海市",
                "ACTIVE"
        ), DemoActors.DEMO_ADMIN_ID);

        assertThat(created.companyName()).isEqualTo("演示展商");
        assertThat(created.status()).isEqualTo("ACTIVE");

        var updated = exhibitorService.update(created.id(), new ExhibitorRequest(
                "演示展商科技有限公司",
                created.creditCode(),
                "李经理",
                "13900000000",
                "sales@example.com",
                "北京市",
                "ACTIVE"
        ), DemoActors.DEMO_ADMIN_ID);

        assertThat(updated.companyName()).isEqualTo("演示展商科技有限公司");
        assertThat(updated.contactName()).isEqualTo("李经理");
        assertThat(updated.contactPhone()).isEqualTo("13900000000");
    }
}
