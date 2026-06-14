package com.sportexpo.exhibitor.order;

import com.sportexpo.exhibitor.TestDatabaseSupport;
import com.sportexpo.exhibitor.common.BusinessStatuses;
import com.sportexpo.exhibitor.common.DemoActors;
import com.sportexpo.exhibitor.event.EventService;
import com.sportexpo.exhibitor.event.dto.CreateEventRequest;
import com.sportexpo.exhibitor.order.dto.CreateParticipationOrderRequest;
import com.sportexpo.exhibitor.pricing.AreaTierService;
import com.sportexpo.exhibitor.pricing.PriceRuleService;
import com.sportexpo.exhibitor.pricing.dto.CreateAreaTierRequest;
import com.sportexpo.exhibitor.pricing.dto.CreatePriceRuleRequest;
import com.sportexpo.exhibitor.selection.SelectionEligibilityRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class ParticipationOrderServiceTest extends TestDatabaseSupport {
    @Autowired EventService eventService;
    @Autowired AreaTierService areaTierService;
    @Autowired PriceRuleService priceRuleService;
    @Autowired ParticipationOrderService orderService;
    @Autowired SelectionEligibilityRepository eligibilityRepository;
    @Autowired JdbcTemplate jdbcTemplate;
    @Value("${app.db.schema}") String schema;

    @Test
    void createsOrderWithCalculatedAmountAndNotOpenEligibility() {
        var event = eventService.create(new CreateEventRequest("体博会", 2026));
        var tier = areaTierService.create(new CreateAreaTierRequest(event.id(), null, new BigDecimal("18.00"), true));
        priceRuleService.create(new CreatePriceRuleRequest(event.id(), null, "STANDARD", new BigDecimal("1200.00"), true));

        var order = orderService.create(new CreateParticipationOrderRequest(
                event.id(), DemoActors.DEMO_EXHIBITOR_ID, null, tier.id(), "STANDARD"
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
        var tier = areaTierService.create(new CreateAreaTierRequest(event.id(), null, new BigDecimal("18.25"), true));
        priceRuleService.create(new CreatePriceRuleRequest(event.id(), null, "STANDARD", new BigDecimal("1200.50"), true));

        var order = orderService.create(new CreateParticipationOrderRequest(
                event.id(), DemoActors.DEMO_EXHIBITOR_ID, null, tier.id(), "STANDARD"
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
        var tier = areaTierService.create(new CreateAreaTierRequest(event.id(), null, new BigDecimal("18.00"), true));
        priceRuleService.create(new CreatePriceRuleRequest(event.id(), null, "STANDARD", new BigDecimal("1200.00"), true));

        var created = orderService.create(new CreateParticipationOrderRequest(
                event.id(), DemoActors.DEMO_EXHIBITOR_ID, null, tier.id(), "STANDARD"
        ));

        var detail = orderService.get(created.id());

        assertThat(detail.id()).isEqualTo(created.id());
        assertThat(detail.createdAt()).isNotNull();
        assertThat(detail.updatedAt()).isNotNull();
    }

    @Test
    void rejectsAreaTierZoneMismatch() {
        var event = eventService.create(new CreateEventRequest("体博会", 2026));
        var zoneId = createZone(event.id());
        var tier = areaTierService.create(new CreateAreaTierRequest(event.id(), zoneId, new BigDecimal("18.00"), true));
        priceRuleService.create(new CreatePriceRuleRequest(event.id(), null, "STANDARD", new BigDecimal("1200.00"), true));

        assertThatThrownBy(() -> orderService.create(new CreateParticipationOrderRequest(
                event.id(), DemoActors.DEMO_EXHIBITOR_ID, null, tier.id(), "STANDARD"
        ))).hasMessageContaining("Area tier does not match order zone");
    }

    @Test
    void rejectsMissingEnabledPriceRule() {
        var event = eventService.create(new CreateEventRequest("体博会", 2026));
        var tier = areaTierService.create(new CreateAreaTierRequest(event.id(), null, new BigDecimal("18.00"), true));

        assertThatThrownBy(() -> orderService.create(new CreateParticipationOrderRequest(
                event.id(), DemoActors.DEMO_EXHIBITOR_ID, null, tier.id(), "STANDARD"
        ))).hasMessageContaining("Enabled price rule does not exist");
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
}
