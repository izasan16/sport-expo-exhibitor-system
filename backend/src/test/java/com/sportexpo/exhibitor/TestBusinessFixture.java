package com.sportexpo.exhibitor;

import com.sportexpo.exhibitor.common.DemoActors;
import com.sportexpo.exhibitor.booth.BoothService;
import com.sportexpo.exhibitor.booth.dto.BoothResponse;
import com.sportexpo.exhibitor.booth.dto.CreateBoothRequest;
import com.sportexpo.exhibitor.contract.ContractRepository;
import com.sportexpo.exhibitor.contract.ContractService;
import com.sportexpo.exhibitor.event.EventService;
import com.sportexpo.exhibitor.event.dto.CreateEventRequest;
import com.sportexpo.exhibitor.event.dto.EventResponse;
import com.sportexpo.exhibitor.order.ParticipationOrderService;
import com.sportexpo.exhibitor.order.dto.CreateParticipationOrderRequest;
import com.sportexpo.exhibitor.order.dto.ParticipationOrderResponse;
import com.sportexpo.exhibitor.payment.PaymentService;
import com.sportexpo.exhibitor.payment.dto.CreatePaymentRequest;
import com.sportexpo.exhibitor.pricing.AreaTierService;
import com.sportexpo.exhibitor.pricing.PriceRuleService;
import com.sportexpo.exhibitor.pricing.dto.CreateAreaTierRequest;
import com.sportexpo.exhibitor.pricing.dto.CreatePriceRuleRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class TestBusinessFixture {
    private final EventService eventService;
    private final AreaTierService areaTierService;
    private final PriceRuleService priceRuleService;
    private final ParticipationOrderService orderService;
    private final ContractRepository contractRepository;
    private final ContractService contractService;
    private final PaymentService paymentService;
    private final BoothService boothService;

    public TestBusinessFixture(EventService eventService, AreaTierService areaTierService,
                               PriceRuleService priceRuleService, ParticipationOrderService orderService,
                               ContractRepository contractRepository, ContractService contractService,
                               PaymentService paymentService, BoothService boothService) {
        this.eventService = eventService;
        this.areaTierService = areaTierService;
        this.priceRuleService = priceRuleService;
        this.orderService = orderService;
        this.contractRepository = contractRepository;
        this.contractService = contractService;
        this.paymentService = paymentService;
        this.boothService = boothService;
    }

    public ParticipationOrderResponse createPendingSalesOrder(String areaSqm, String unitPrice, String boothType) {
        var event = eventService.create(new CreateEventRequest("体博会", 2026));
        return createPendingSalesOrderForEvent(event.id(), areaSqm, unitPrice, boothType);
    }

    public ParticipationOrderResponse createPendingSalesOrderForEvent(UUID eventId, String areaSqm, String unitPrice,
                                                                      String boothType) {
        var area = new BigDecimal(areaSqm);
        var tier = areaTierService.list(eventId).stream()
                .filter(existing -> existing.zoneId() == null)
                .filter(existing -> existing.areaSqm().compareTo(area) == 0)
                .findFirst()
                .orElseGet(() -> areaTierService.create(new CreateAreaTierRequest(eventId, null, area, true)));
        var price = new BigDecimal(unitPrice);
        var hasPriceRule = priceRuleService.list(eventId).stream()
                .anyMatch(existing -> existing.zoneId() == null
                        && existing.boothType().equals(boothType)
                        && existing.enabled());
        if (!hasPriceRule) {
            priceRuleService.create(new CreatePriceRuleRequest(eventId, null, boothType, price, true));
        }
        return orderService.create(new CreateParticipationOrderRequest(
                eventId, DemoActors.DEMO_EXHIBITOR_ID, null, tier.id(), boothType
        ));
    }

    public ParticipationOrderResponse createPendingPaymentOrder(String areaSqm, String unitPrice, String boothType) {
        var order = createPendingSalesOrder(areaSqm, unitPrice, boothType);
        var confirmed = orderService.confirmSales(order.id(), DemoActors.DEMO_ADMIN_ID);
        var contract = contractRepository.findByOrderId(confirmed.id()).orElseThrow();
        contractService.markOfflineSigned(contract.getId(), DemoActors.DEMO_ADMIN_ID);
        return orderService.get(order.id());
    }

    public ParticipationOrderResponse createPaidOrder(String areaSqm, String unitPrice, String boothType) {
        var event = eventService.create(new CreateEventRequest("体博会", 2026));
        return createPaidOrderForEvent(event.id(), areaSqm, unitPrice, boothType);
    }

    public ParticipationOrderResponse createPaidOrderForEvent(UUID eventId, String areaSqm, String unitPrice,
                                                              String boothType) {
        var order = createPendingPaymentOrderForEvent(eventId, areaSqm, unitPrice, boothType);
        var payment = paymentService.create(new CreatePaymentRequest(
                order.id(), order.totalAmount(), "OFFLINE_TRANSFER"));
        paymentService.confirm(payment.id(), DemoActors.DEMO_ADMIN_ID);
        return orderService.get(order.id());
    }

    public BoothResponse createBooth(UUID eventId, String boothNo, String areaSqm, String boothType) {
        return boothService.create(new CreateBoothRequest(
                eventId,
                boothNo,
                new BigDecimal(areaSqm),
                boothType
        ));
    }

    public EventResponse createEvent() {
        return eventService.create(new CreateEventRequest("体博会", 2026));
    }

    public ParticipationOrderResponse createPendingPaymentOrderForEvent(UUID eventId, String areaSqm, String unitPrice,
                                                                        String boothType) {
        var order = createPendingSalesOrderForEvent(eventId, areaSqm, unitPrice, boothType);
        var confirmed = orderService.confirmSales(order.id(), DemoActors.DEMO_ADMIN_ID);
        var contract = contractRepository.findByOrderId(confirmed.id()).orElseThrow();
        contractService.markOfflineSigned(contract.getId(), DemoActors.DEMO_ADMIN_ID);
        return orderService.get(order.id());
    }
}
