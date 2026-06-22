package com.sportexpo.exhibitor.pricing;

import com.sportexpo.exhibitor.common.DemoActors;
import com.sportexpo.exhibitor.TestDatabaseSupport;
import com.sportexpo.exhibitor.event.EventService;
import com.sportexpo.exhibitor.event.dto.CreateEventRequest;
import com.sportexpo.exhibitor.pricing.dto.CreateAreaTierRequest;
import com.sportexpo.exhibitor.pricing.dto.CreatePriceRuleRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class PricingServiceTest extends TestDatabaseSupport {
    @Autowired EventService eventService;
    @Autowired AreaTierService areaTierService;
    @Autowired PriceRuleService priceRuleService;

    @Test
    void createsTierAndPriceRuleForEvent() {
        var event = eventService.create(new CreateEventRequest("体博会", 2026));

        var tier = areaTierService.create(new CreateAreaTierRequest(event.id(), null, new BigDecimal("18.00"), true),
                DemoActors.DEMO_ADMIN_ID);
        var price = priceRuleService.create(new CreatePriceRuleRequest(event.id(), null, "STANDARD", new BigDecimal("1200.00"), true),
                DemoActors.DEMO_ADMIN_ID);

        assertThat(tier.areaSqm()).isEqualByComparingTo("18.00");
        assertThat(price.unitPrice()).isEqualByComparingTo("1200.00");
        assertThat(areaTierService.list(event.id())).hasSize(1);
        assertThat(priceRuleService.list(event.id())).hasSize(1);
    }

    @Test
    void rejectsDuplicateEnabledPriceRuleForSameEventZoneAndBoothType() {
        var event = eventService.create(new CreateEventRequest("体博会", 2026));
        priceRuleService.create(new CreatePriceRuleRequest(event.id(), null, "STANDARD", new BigDecimal("1200.00"), true),
                DemoActors.DEMO_ADMIN_ID);

        assertThatThrownBy(() -> priceRuleService.create(new CreatePriceRuleRequest(event.id(), null, "STANDARD", new BigDecimal("1300.00"), true),
                DemoActors.DEMO_ADMIN_ID))
                .hasMessageContaining("Enabled price rule already exists");
    }
}
