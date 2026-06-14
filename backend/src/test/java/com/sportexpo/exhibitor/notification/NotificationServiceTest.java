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
}
