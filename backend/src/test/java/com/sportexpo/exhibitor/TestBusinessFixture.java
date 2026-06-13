package com.sportexpo.exhibitor;

import com.sportexpo.exhibitor.common.DemoActors;
import com.sportexpo.exhibitor.contract.ContractRepository;
import com.sportexpo.exhibitor.contract.ContractService;
import com.sportexpo.exhibitor.event.EventService;
import com.sportexpo.exhibitor.event.dto.CreateEventRequest;
import com.sportexpo.exhibitor.order.ParticipationOrderService;
import com.sportexpo.exhibitor.order.dto.CreateParticipationOrderRequest;
import com.sportexpo.exhibitor.order.dto.ParticipationOrderResponse;
import com.sportexpo.exhibitor.pricing.AreaTierService;
import com.sportexpo.exhibitor.pricing.PriceRuleService;
import com.sportexpo.exhibitor.pricing.dto.CreateAreaTierRequest;
import com.sportexpo.exhibitor.pricing.dto.CreatePriceRuleRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class TestBusinessFixture {
    private final EventService eventService;
    private final AreaTierService areaTierService;
    private final PriceRuleService priceRuleService;
    private final ParticipationOrderService orderService;
    private final ContractRepository contractRepository;
    private final ContractService contractService;

    public TestBusinessFixture(EventService eventService, AreaTierService areaTierService,
                               PriceRuleService priceRuleService, ParticipationOrderService orderService,
                               ContractRepository contractRepository, ContractService contractService) {
        this.eventService = eventService;
        this.areaTierService = areaTierService;
        this.priceRuleService = priceRuleService;
        this.orderService = orderService;
        this.contractRepository = contractRepository;
        this.contractService = contractService;
    }

    public ParticipationOrderResponse createPendingSalesOrder(String areaSqm, String unitPrice, String boothType) {
        var event = eventService.create(new CreateEventRequest("体博会", 2026));
        var tier = areaTierService.create(new CreateAreaTierRequest(event.id(), null, new BigDecimal(areaSqm), true));
        priceRuleService.create(new CreatePriceRuleRequest(event.id(), null, boothType, new BigDecimal(unitPrice), true));
        return orderService.create(new CreateParticipationOrderRequest(
                event.id(), DemoActors.DEMO_EXHIBITOR_ID, null, tier.id(), boothType
        ));
    }

    public ParticipationOrderResponse createPendingPaymentOrder(String areaSqm, String unitPrice, String boothType) {
        var order = createPendingSalesOrder(areaSqm, unitPrice, boothType);
        var confirmed = orderService.confirmSales(order.id(), DemoActors.DEMO_ADMIN_ID);
        var contract = contractRepository.findByOrderId(confirmed.id()).orElseThrow();
        contractService.markOfflineSigned(contract.getId(), DemoActors.DEMO_ADMIN_ID);
        return orderService.get(order.id());
    }
}
