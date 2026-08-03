package com.sportexpo.exhibitor.order;

import com.sportexpo.exhibitor.TestBusinessFixture;
import com.sportexpo.exhibitor.TestDatabaseSupport;
import com.sportexpo.exhibitor.common.BusinessException;
import com.sportexpo.exhibitor.common.BusinessStatuses;
import com.sportexpo.exhibitor.common.DemoActors;
import com.sportexpo.exhibitor.event.EventService;
import com.sportexpo.exhibitor.event.dto.CreateEventRequest;
import com.sportexpo.exhibitor.eventcategory.EventCategoryService;
import com.sportexpo.exhibitor.eventcategory.dto.EventCategoryRequest;
import com.sportexpo.exhibitor.exhibitor.ExhibitorService;
import com.sportexpo.exhibitor.exhibitor.dto.ExhibitorRequest;
import com.sportexpo.exhibitor.notification.NotificationService;
import com.sportexpo.exhibitor.notification.NotificationTypes;
import com.sportexpo.exhibitor.order.dto.CreateParticipationOrderRequest;
import com.sportexpo.exhibitor.pricing.AreaTierService;
import com.sportexpo.exhibitor.pricing.PriceRuleService;
import com.sportexpo.exhibitor.pricing.dto.CreateAreaTierRequest;
import com.sportexpo.exhibitor.pricing.dto.CreatePriceRuleRequest;
import com.sportexpo.exhibitor.registration.RegistrationService;
import com.sportexpo.exhibitor.registration.dto.RegistrationRequest;
import com.sportexpo.exhibitor.selection.SelectionEligibilityRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;

@SpringBootTest
class ParticipationOrderServiceTest extends TestDatabaseSupport {
    @Autowired TestBusinessFixture fixture;
    @Autowired EventService eventService;
    @Autowired AreaTierService areaTierService;
    @Autowired PriceRuleService priceRuleService;
    @Autowired ParticipationOrderService orderService;
    @Autowired EventCategoryService eventCategoryService;
    @Autowired RegistrationService registrationService;
    @Autowired ExhibitorService exhibitorService;
    @Autowired SelectionEligibilityRepository eligibilityRepository;
    @Autowired JdbcTemplate jdbcTemplate;
    @SpyBean NotificationService notificationService;
    @Value("${app.db.schema}") String schema;

    @Test
    void createsOrderWithCalculatedAmountAndNotOpenEligibility() {
        var event = eventService.create(new CreateEventRequest("体博会", 2026));
        var tier = areaTierService.create(new CreateAreaTierRequest(event.id(), null, new BigDecimal("18.00"), true), DemoActors.DEMO_ADMIN_ID);
        priceRuleService.create(new CreatePriceRuleRequest(event.id(), null, "STANDARD", new BigDecimal("1200.00"), true), DemoActors.DEMO_ADMIN_ID);
        createApprovedRegistration(event.id(), DemoActors.DEMO_EXHIBITOR_ID, null);

        var order = orderService.create(new CreateParticipationOrderRequest(
                event.id(), DemoActors.DEMO_EXHIBITOR_ID, null, null, tier.id(), "STANDARD"
        ));

        assertThat(order.areaSqm()).isEqualByComparingTo("18.00");
        assertThat(order.areaFee()).isEqualByComparingTo("21600.00");
        assertThat(order.totalAmount()).isEqualByComparingTo("21600.00");
        assertThat(order.status()).isEqualTo(BusinessStatuses.ORDER_PENDING_SALES_CONFIRM);
        var eligibility = eligibilityRepository.findByOrderId(order.id()).orElseThrow();
        assertThat(eligibility.getStatus()).isEqualTo(BusinessStatuses.ELIGIBILITY_NOT_OPEN);
    }

    @Test
    void roundsCalculatedAmountsToMoneyScale() {
        var event = eventService.create(new CreateEventRequest("体博会", 2026));
        var tier = areaTierService.create(new CreateAreaTierRequest(event.id(), null, new BigDecimal("18.25"), true), DemoActors.DEMO_ADMIN_ID);
        priceRuleService.create(new CreatePriceRuleRequest(event.id(), null, "STANDARD", new BigDecimal("1200.50"), true), DemoActors.DEMO_ADMIN_ID);
        createApprovedRegistration(event.id(), DemoActors.DEMO_EXHIBITOR_ID, null);

        var order = orderService.create(new CreateParticipationOrderRequest(
                event.id(), DemoActors.DEMO_EXHIBITOR_ID, null, null, tier.id(), "STANDARD"
        ));

        assertThat(order.areaFee()).isEqualByComparingTo("21909.13");
        assertThat(order.totalAmount()).isEqualByComparingTo("21909.13");
        assertThat(order.serviceFee()).isEqualTo(new BigDecimal("0.00"));
        assertThat(order.discountAmount()).isEqualTo(new BigDecimal("0.00"));
        assertThat(order.adjustmentAmount()).isEqualTo(new BigDecimal("0.00"));
    }

    @Test
    void returnsOrderDetailWithCreatedAndUpdatedTimes() {
        var event = eventService.create(new CreateEventRequest("体博会", 2026));
        var tier = areaTierService.create(new CreateAreaTierRequest(event.id(), null, new BigDecimal("18.00"), true), DemoActors.DEMO_ADMIN_ID);
        priceRuleService.create(new CreatePriceRuleRequest(event.id(), null, "STANDARD", new BigDecimal("1200.00"), true), DemoActors.DEMO_ADMIN_ID);
        createApprovedRegistration(event.id(), DemoActors.DEMO_EXHIBITOR_ID, null);

        var created = orderService.create(new CreateParticipationOrderRequest(
                event.id(), DemoActors.DEMO_EXHIBITOR_ID, null, null, tier.id(), "STANDARD"
        ));

        var detail = orderService.get(created.id());

        assertThat(detail.id()).isEqualTo(created.id());
        assertThat(detail.createdAt()).isNotNull();
        assertThat(detail.updatedAt()).isNotNull();
    }

    @Test
    void batchConfirmSalesKeepsSuccessfulOrdersWhenOtherItemsFail() {
        var ready = fixture.createPendingSalesOrder("18.00", "1200.00", "STANDARD");
        var alreadyConfirmed = fixture.createPendingSalesOrder("18.00", "1200.00", "STANDARD");
        orderService.confirmSales(alreadyConfirmed.id(), DemoActors.DEMO_ADMIN_ID);
        var missingOrderId = UUID.randomUUID();

        var result = orderService.batchConfirmSales(
                List.of(ready.id(), alreadyConfirmed.id(), missingOrderId),
                DemoActors.DEMO_ADMIN_ID);

        assertThat(result.successCount()).isEqualTo(1);
        assertThat(result.failureCount()).isEqualTo(2);
        assertThat(result.failures())
                .extracting("orderId")
                .containsExactlyInAnyOrder(alreadyConfirmed.id(), missingOrderId);
        assertThat(orderService.get(ready.id()).status()).isEqualTo(BusinessStatuses.ORDER_PENDING_CONTRACT);
    }

    @Test
    void confirmSalesCreatesChineseExhibitorNotification() {
        var order = fixture.createPendingSalesOrder("18.00", "1200.00", "STANDARD");

        orderService.confirmSales(order.id(), DemoActors.DEMO_ADMIN_ID);

        assertThat(notificationService.list(order.exhibitorId()))
                .filteredOn(notification -> notification.type().equals(NotificationTypes.ORDER_SALES_CONFIRMED))
                .filteredOn(notification -> order.id().equals(notification.relatedOrderId()))
                .anySatisfy(notification -> {
                    assertThat(notification.title()).isEqualTo("订单已确认");
                    assertThat(notification.content()).isEqualTo("您的参展订单已由销售确认，请继续完成合同确认。");
                    assertThat(notification.relatedOrderId()).isEqualTo(order.id());
                });
    }

    @Test
    void batchConfirmSalesKeepsBusinessReasonsButHidesUnexpectedRuntimeMessages() {
        var ready = fixture.createPendingSalesOrder("18.00", "1200.00", "STANDARD");
        var alreadyConfirmed = fixture.createPendingSalesOrder("18.00", "1200.00", "STANDARD");
        orderService.confirmSales(alreadyConfirmed.id(), DemoActors.DEMO_ADMIN_ID);
        doThrow(new IllegalStateException("internal notification outage"))
                .when(notificationService)
                .create(eq(ready.eventId()), anyString(), eq(ready.exhibitorId()), anyString(), anyString(), anyString(),
                        isNull(), isNull(), eq(ready.id()));

        var result = orderService.batchConfirmSales(
                List.of(alreadyConfirmed.id(), ready.id()),
                DemoActors.DEMO_ADMIN_ID);

        assertThat(result.successCount()).isZero();
        assertThat(result.failureCount()).isEqualTo(2);
        assertThat(result.failures())
                .anySatisfy(failure -> {
                    assertThat(failure.orderId()).isEqualTo(alreadyConfirmed.id());
                    assertThat(failure.reason()).isEqualTo("Order is not pending sales confirmation");
                })
                .anySatisfy(failure -> {
                    assertThat(failure.orderId()).isEqualTo(ready.id());
                    assertThat(failure.reason()).isEqualTo("Order confirmation failed");
                });
        assertThat(orderService.get(ready.id()).status()).isEqualTo(BusinessStatuses.ORDER_PENDING_SALES_CONFIRM);
    }

    @Test
    void rejectsAreaTierZoneMismatch() {
        var event = eventService.create(new CreateEventRequest("体博会", 2026));
        var zoneId = createZone(event.id());
        var tier = areaTierService.create(new CreateAreaTierRequest(event.id(), zoneId, new BigDecimal("18.00"), true), DemoActors.DEMO_ADMIN_ID);
        priceRuleService.create(new CreatePriceRuleRequest(event.id(), null, "STANDARD", new BigDecimal("1200.00"), true), DemoActors.DEMO_ADMIN_ID);
        createApprovedRegistration(event.id(), DemoActors.DEMO_EXHIBITOR_ID, null);

        assertThatThrownBy(() -> orderService.create(new CreateParticipationOrderRequest(
                event.id(), DemoActors.DEMO_EXHIBITOR_ID, null, null, tier.id(), "STANDARD"
        ))).hasMessageContaining("Area tier does not match order zone");
    }

    @Test
    void rejectsMissingEnabledPriceRule() {
        var event = eventService.create(new CreateEventRequest("体博会", 2026));
        var tier = areaTierService.create(new CreateAreaTierRequest(event.id(), null, new BigDecimal("18.00"), true), DemoActors.DEMO_ADMIN_ID);
        createApprovedRegistration(event.id(), DemoActors.DEMO_EXHIBITOR_ID, null);

        assertThatThrownBy(() -> orderService.create(new CreateParticipationOrderRequest(
                event.id(), DemoActors.DEMO_EXHIBITOR_ID, null, null, tier.id(), "STANDARD"
        ))).hasMessageContaining("Enabled price rule does not exist");
    }

    @Test
    void createOrderFailsWithoutApprovedRegistration() {
        var event = eventService.create(new CreateEventRequest("报名校验测试展", 2026));
        var tier = areaTierService.create(new CreateAreaTierRequest(event.id(), null, new BigDecimal("18.00"), true), DemoActors.DEMO_ADMIN_ID);
        priceRuleService.create(new CreatePriceRuleRequest(event.id(), null, "STANDARD", new BigDecimal("1200.00"), true), DemoActors.DEMO_ADMIN_ID);

        assertThatThrownBy(() -> orderService.create(new CreateParticipationOrderRequest(
                event.id(), DemoActors.DEMO_EXHIBITOR_ID, null, null, tier.id(), "STANDARD"
        ))).isInstanceOf(BusinessException.class)
                .hasMessage("Registration does not exist");
    }

    @Test
    void createOrderBindsApprovedRegistration() {
        var event = eventService.create(new CreateEventRequest("报名绑定测试展", 2026));
        var tier = areaTierService.create(new CreateAreaTierRequest(event.id(), null, new BigDecimal("18.00"), true), DemoActors.DEMO_ADMIN_ID);
        priceRuleService.create(new CreatePriceRuleRequest(event.id(), null, "STANDARD", new BigDecimal("1200.00"), true), DemoActors.DEMO_ADMIN_ID);
        var registration = createApprovedRegistration(event.id(), DemoActors.DEMO_EXHIBITOR_ID, null);

        var order = orderService.create(new CreateParticipationOrderRequest(
                event.id(), DemoActors.DEMO_EXHIBITOR_ID, registration.id(), null, tier.id(), "STANDARD"
        ));

        assertThat(order.registrationId()).isEqualTo(registration.id());
    }

    @Test
    void createOrderAutoBindsOnlyApprovedRegistration() {
        var event = eventService.create(new CreateEventRequest("报名自动绑定测试展", 2026));
        var tier = areaTierService.create(new CreateAreaTierRequest(event.id(), null, new BigDecimal("18.00"), true), DemoActors.DEMO_ADMIN_ID);
        priceRuleService.create(new CreatePriceRuleRequest(event.id(), null, "STANDARD", new BigDecimal("1200.00"), true), DemoActors.DEMO_ADMIN_ID);
        var registration = createApprovedRegistration(event.id(), DemoActors.DEMO_EXHIBITOR_ID, null);

        var order = orderService.create(new CreateParticipationOrderRequest(
                event.id(), DemoActors.DEMO_EXHIBITOR_ID, null, null, tier.id(), "STANDARD"
        ));

        assertThat(order.registrationId()).isEqualTo(registration.id());
    }

    @Test
    void createOrderRejectsInactiveExhibitor() {
        var event = eventService.create(new CreateEventRequest("非活跃展商测试展", 2026));
        var tier = areaTierService.create(new CreateAreaTierRequest(event.id(), null, new BigDecimal("18.00"), true), DemoActors.DEMO_ADMIN_ID);
        priceRuleService.create(new CreatePriceRuleRequest(event.id(), null, "STANDARD", new BigDecimal("1200.00"), true), DemoActors.DEMO_ADMIN_ID);
        var suspended = exhibitorService.create(new ExhibitorRequest(
                "暂停展商", "SUSPENDED-" + UUID.randomUUID(), "李总", "13600000000",
                "suspended@example.com", "北京市", BusinessStatuses.EXHIBITOR_SUSPENDED
        ), DemoActors.DEMO_ADMIN_ID);

        assertThatThrownBy(() -> orderService.create(new CreateParticipationOrderRequest(
                event.id(), suspended.id(), null, null, tier.id(), "STANDARD"
        ))).isInstanceOf(BusinessException.class)
                .hasMessage("Exhibitor does not exist");
    }

    @Test
    void createOrderRejectsRegistrationThatIsNotApproved() {
        var event = eventService.create(new CreateEventRequest("未审核报名测试展", 2026));
        var tier = areaTierService.create(new CreateAreaTierRequest(event.id(), null, new BigDecimal("18.00"), true), DemoActors.DEMO_ADMIN_ID);
        priceRuleService.create(new CreatePriceRuleRequest(event.id(), null, "STANDARD", new BigDecimal("1200.00"), true), DemoActors.DEMO_ADMIN_ID);
        ensureEventCategory(event.id());
        var draft = registrationService.create(new RegistrationRequest(
                event.id(), DemoActors.DEMO_EXHIBITOR_ID, "SPORTS_EQUIPMENT", null, new BigDecimal("18.00"),
                "王老板", "13800000000", "尚未审核"
        ), DemoActors.DEMO_EXHIBITOR_ID);

        assertThatThrownBy(() -> orderService.create(new CreateParticipationOrderRequest(
                event.id(), DemoActors.DEMO_EXHIBITOR_ID, draft.id(), null, tier.id(), "STANDARD"
        ))).isInstanceOf(BusinessException.class)
                .hasMessage("Registration is not approved");
    }

    @Test
    void createOrderRequiresExplicitChoiceWhenMultipleApprovedRegistrationsExist() {
        var event = eventService.create(new CreateEventRequest("多报名测试展", 2026));
        var tier = areaTierService.create(new CreateAreaTierRequest(event.id(), null, new BigDecimal("18.00"), true), DemoActors.DEMO_ADMIN_ID);
        priceRuleService.create(new CreatePriceRuleRequest(event.id(), null, "STANDARD", new BigDecimal("1200.00"), true), DemoActors.DEMO_ADMIN_ID);
        createApprovedRegistration(event.id(), DemoActors.DEMO_EXHIBITOR_ID, null);
        createApprovedRegistration(event.id(), DemoActors.DEMO_EXHIBITOR_ID, null);

        assertThatThrownBy(() -> orderService.create(new CreateParticipationOrderRequest(
                event.id(), DemoActors.DEMO_EXHIBITOR_ID, null, null, tier.id(), "STANDARD"
        ))).isInstanceOf(BusinessException.class)
                .hasMessage("Multiple approved registrations exist; choose one");
    }

    @Test
    void createOrderRejectsRegistrationFromDifferentEventOrExhibitor() {
        var event = eventService.create(new CreateEventRequest("订单展会", 2026));
        var otherEvent = eventService.create(new CreateEventRequest("报名展会", 2026));
        var tier = areaTierService.create(new CreateAreaTierRequest(event.id(), null, new BigDecimal("18.00"), true), DemoActors.DEMO_ADMIN_ID);
        priceRuleService.create(new CreatePriceRuleRequest(event.id(), null, "STANDARD", new BigDecimal("1200.00"), true), DemoActors.DEMO_ADMIN_ID);
        var otherEventRegistration = createApprovedRegistration(otherEvent.id(), DemoActors.DEMO_EXHIBITOR_ID, null);
        var otherExhibitor = exhibitorService.create(new ExhibitorRequest(
                "其他展商", "CREDIT-" + UUID.randomUUID(), "张总", "13700000000",
                "other@example.com", "上海市", "ACTIVE"
        ), DemoActors.DEMO_ADMIN_ID);
        var otherExhibitorRegistration = createApprovedRegistration(event.id(), otherExhibitor.id(), null);

        assertThatThrownBy(() -> orderService.create(new CreateParticipationOrderRequest(
                event.id(), DemoActors.DEMO_EXHIBITOR_ID, otherEventRegistration.id(), null, tier.id(), "STANDARD"
        ))).hasMessage("Registration does not belong to this event");

        assertThatThrownBy(() -> orderService.create(new CreateParticipationOrderRequest(
                event.id(), DemoActors.DEMO_EXHIBITOR_ID, otherExhibitorRegistration.id(), null, tier.id(), "STANDARD"
        ))).hasMessage("Registration does not belong to this exhibitor");
    }

    @Test
    void createOrderRejectsZoneOutsideRegistrationScope() {
        var event = eventService.create(new CreateEventRequest("报名展区测试展", 2026));
        var approvedZone = createZone(event.id());
        var orderZone = createZone(event.id());
        var tier = areaTierService.create(new CreateAreaTierRequest(event.id(), orderZone, new BigDecimal("18.00"), true), DemoActors.DEMO_ADMIN_ID);
        priceRuleService.create(new CreatePriceRuleRequest(event.id(), orderZone, "STANDARD", new BigDecimal("1200.00"), true), DemoActors.DEMO_ADMIN_ID);
        var registration = createApprovedRegistration(event.id(), DemoActors.DEMO_EXHIBITOR_ID, approvedZone);

        assertThatThrownBy(() -> orderService.create(new CreateParticipationOrderRequest(
                event.id(), DemoActors.DEMO_EXHIBITOR_ID, registration.id(), orderZone, tier.id(), "STANDARD"
        ))).hasMessage("Order zone is outside approved registration scope");
    }

    private UUID createZone(UUID eventId) {
        var pavilionId = UUID.randomUUID();
        var zoneId = UUID.randomUUID();
        var suffix = zoneId.toString().substring(0, 8);
        jdbcTemplate.update("""
                        insert into %s.pavilions (id, event_id, name, code, created_at, updated_at)
                        values (?, ?, ?, ?, now(), now())
                        """.formatted(schema),
                pavilionId, eventId, "测试馆-" + suffix, "P-" + suffix);
        jdbcTemplate.update("""
                        insert into %s.zones (id, event_id, pavilion_id, name, code, category_code, created_at, updated_at)
                        values (?, ?, ?, ?, ?, ?, now(), now())
                        """.formatted(schema),
                zoneId, eventId, pavilionId, "测试分区-" + suffix, "Z-" + suffix, "STANDARD");
        return zoneId;
    }

    private com.sportexpo.exhibitor.registration.dto.RegistrationResponse createApprovedRegistration(
            UUID eventId, UUID exhibitorId, UUID zoneId) {
        ensureEventCategory(eventId);
        var draft = registrationService.create(new RegistrationRequest(
                eventId, exhibitorId, "SPORTS_EQUIPMENT", zoneId, new BigDecimal("18.00"),
                "王老板", "13800000000", "报名参展"
        ), exhibitorId);
        registrationService.submit(draft.id(), exhibitorId);
        return registrationService.approve(draft.id(), DemoActors.DEMO_ADMIN_ID);
    }

    private void ensureEventCategory(UUID eventId) {
        var exists = eventCategoryService.list(eventId, true).stream()
                .anyMatch(category -> "SPORTS_EQUIPMENT".equals(category.code()));
        if (!exists) {
            eventCategoryService.create(new EventCategoryRequest(eventId, "SPORTS_EQUIPMENT", "体育用品", true, 10),
                    DemoActors.DEMO_ADMIN_ID);
        }
    }
}
