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
import com.sportexpo.exhibitor.eventcategory.EventCategoryService;
import com.sportexpo.exhibitor.eventcategory.dto.EventCategoryRequest;
import com.sportexpo.exhibitor.order.ParticipationOrderService;
import com.sportexpo.exhibitor.order.dto.CreateParticipationOrderRequest;
import com.sportexpo.exhibitor.order.dto.ParticipationOrderResponse;
import com.sportexpo.exhibitor.payment.PaymentService;
import com.sportexpo.exhibitor.payment.dto.CreatePaymentRequest;
import com.sportexpo.exhibitor.pricing.AreaTierService;
import com.sportexpo.exhibitor.pricing.PriceRuleService;
import com.sportexpo.exhibitor.pricing.dto.CreateAreaTierRequest;
import com.sportexpo.exhibitor.pricing.dto.CreatePriceRuleRequest;
import com.sportexpo.exhibitor.registration.RegistrationService;
import com.sportexpo.exhibitor.registration.dto.RegistrationRequest;
import com.sportexpo.exhibitor.selection.BoothSelectionService;
import com.sportexpo.exhibitor.selection.dto.BoothSelectionResponse;
import com.sportexpo.exhibitor.selection.dto.CreateBoothSelectionRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Component
public class TestBusinessFixture {
    private final EventService eventService;
    private final EventCategoryService eventCategoryService;
    private final RegistrationService registrationService;
    private final AreaTierService areaTierService;
    private final PriceRuleService priceRuleService;
    private final ParticipationOrderService orderService;
    private final ContractRepository contractRepository;
    private final ContractService contractService;
    private final PaymentService paymentService;
    private final BoothService boothService;
    private final BoothSelectionService selectionService;
    private final JdbcTemplate jdbcTemplate;
    private final String schema;

    public TestBusinessFixture(EventService eventService, EventCategoryService eventCategoryService,
                               RegistrationService registrationService, AreaTierService areaTierService,
                               PriceRuleService priceRuleService, ParticipationOrderService orderService,
                               ContractRepository contractRepository, ContractService contractService,
                               PaymentService paymentService, BoothService boothService,
                               BoothSelectionService selectionService, JdbcTemplate jdbcTemplate,
                               @Value("${app.db.schema}") String schema) {
        this.eventService = eventService;
        this.eventCategoryService = eventCategoryService;
        this.registrationService = registrationService;
        this.areaTierService = areaTierService;
        this.priceRuleService = priceRuleService;
        this.orderService = orderService;
        this.contractRepository = contractRepository;
        this.contractService = contractService;
        this.paymentService = paymentService;
        this.boothService = boothService;
        this.selectionService = selectionService;
        this.jdbcTemplate = jdbcTemplate;
        this.schema = schema;
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
        ensureApprovedRegistration(eventId);
        return orderService.create(new CreateParticipationOrderRequest(
                eventId, DemoActors.DEMO_EXHIBITOR_ID, null, null, tier.id(), boothType
        ));
    }

    private void ensureApprovedRegistration(UUID eventId) {
        var hasCategory = eventCategoryService.list(eventId, true).stream()
                .anyMatch(category -> "SPORTS_EQUIPMENT".equals(category.code()));
        if (!hasCategory) {
            eventCategoryService.create(new EventCategoryRequest(eventId, "SPORTS_EQUIPMENT", "体育用品", true, 10),
                    DemoActors.DEMO_ADMIN_ID);
        }
        var hasApproved = registrationService.list(eventId, DemoActors.DEMO_EXHIBITOR_ID, "APPROVED").stream()
                .findAny()
                .isPresent();
        if (hasApproved) {
            return;
        }
        var draft = registrationService.create(new RegistrationRequest(
                eventId, DemoActors.DEMO_EXHIBITOR_ID, "SPORTS_EQUIPMENT", null, new BigDecimal("18.00"),
                "王老板", "13800000000", "测试报名"
        ), DemoActors.DEMO_EXHIBITOR_ID);
        registrationService.submit(draft.id(), DemoActors.DEMO_EXHIBITOR_ID);
        registrationService.approve(draft.id(), DemoActors.DEMO_ADMIN_ID);
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

    public BoothSelectionResponse createPendingSelection(String areaSqm, String boothType) {
        var order = createPaidOrder(areaSqm, "1200.00", boothType);
        var booth = createBooth(order.eventId(), "A-" + UUID.randomUUID(), areaSqm, boothType);
        return selectionService.submit(new CreateBoothSelectionRequest(
                order.id(), order.exhibitorId(), List.of(booth.id())));
    }

    public BoothSelectionResponse createExpiredPendingSelection(String areaSqm, String boothType) {
        var selection = createPendingSelection(areaSqm, boothType);
        expireLockForTest(selection.id());
        return selection;
    }

    public void expireLockForTest(UUID selectionId) {
        var qualifiedSelections = schema + ".booth_selections";
        var qualifiedLocks = schema + ".booth_locks";
        jdbcTemplate.update("update " + qualifiedSelections + " set lock_expires_at = now() - interval '1 minute' where id = ?",
                selectionId);
        jdbcTemplate.update("update " + qualifiedLocks + " set lock_expires_at = now() - interval '1 minute' where selection_id = ?",
                selectionId);
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
