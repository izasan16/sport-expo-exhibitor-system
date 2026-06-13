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
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ParticipationOrderServiceTest extends TestDatabaseSupport {
    @Autowired EventService eventService;
    @Autowired AreaTierService areaTierService;
    @Autowired PriceRuleService priceRuleService;
    @Autowired ParticipationOrderService orderService;
    @Autowired SelectionEligibilityRepository eligibilityRepository;

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
}
