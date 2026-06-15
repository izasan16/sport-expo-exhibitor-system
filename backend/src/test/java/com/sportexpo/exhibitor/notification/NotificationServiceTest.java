package com.sportexpo.exhibitor.notification;

import com.sportexpo.exhibitor.TestBusinessFixture;
import com.sportexpo.exhibitor.TestDatabaseSupport;
import com.sportexpo.exhibitor.common.DemoActors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class NotificationServiceTest extends TestDatabaseSupport {
    @Autowired TestBusinessFixture fixture;
    @Autowired NotificationService notificationService;

    @Test
    void createsListsAndMarksNotificationRead() {
        var event = fixture.createEvent();

        var created = notificationService.create(
                event.id(),
                "EXHIBITOR",
                DemoActors.DEMO_EXHIBITOR_ID,
                "Payment failed",
                "户名不符",
                NotificationTypes.PAYMENT_FAILED
        );

        assertThat(created.readAt()).isNull();
        assertThat(notificationService.list(DemoActors.DEMO_EXHIBITOR_ID))
                .extracting("id")
                .contains(created.id());

        var read = notificationService.markRead(created.id(), DemoActors.DEMO_EXHIBITOR_ID);

        assertThat(read.readAt()).isNotNull();
    }

    @Test
    void createsNotificationWithRelatedObjectFields() {
        var event = fixture.createEvent();

        var created = notificationService.create(
                event.id(),
                "EXHIBITOR",
                DemoActors.DEMO_EXHIBITOR_ID,
                "报名审核通过",
                "可以创建订单",
                NotificationTypes.REGISTRATION_APPROVED,
                "Registration",
                DemoActors.DEMO_EXHIBITOR_ID,
                null
        );

        assertThat(created.relatedType()).isEqualTo("Registration");
        assertThat(created.relatedId()).isEqualTo(DemoActors.DEMO_EXHIBITOR_ID);
        assertThat(created.relatedOrderId()).isNull();
    }
}
