package com.sportexpo.exhibitor.pricing;

import com.sportexpo.exhibitor.audit.AuditService;
import com.sportexpo.exhibitor.common.BusinessException;
import com.sportexpo.exhibitor.common.DemoActors;
import com.sportexpo.exhibitor.event.EventRepository;
import com.sportexpo.exhibitor.pricing.dto.CreatePriceRuleRequest;
import com.sportexpo.exhibitor.pricing.dto.PriceRuleResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class PriceRuleService {
    private final PriceRuleRepository repository;
    private final EventRepository eventRepository;
    private final AuditService auditService;

    public PriceRuleService(PriceRuleRepository repository, EventRepository eventRepository, AuditService auditService) {
        this.repository = repository;
        this.eventRepository = eventRepository;
        this.auditService = auditService;
    }

    @Transactional
    public PriceRuleResponse create(CreatePriceRuleRequest request) {
        eventRepository.findById(request.eventId())
                .orElseThrow(() -> new BusinessException("Event does not exist"));
        if (request.enabled() && repository.existsByEventIdAndZoneIdAndBoothTypeAndEnabled(
                request.eventId(), request.zoneId(), request.boothType(), true)) {
            throw new BusinessException("Enabled price rule already exists");
        }
        var priceRule = repository.save(new PriceRule(
                UUID.randomUUID(),
                request.eventId(),
                request.zoneId(),
                request.boothType(),
                request.unitPrice(),
                request.enabled()
        ));
        auditService.record(priceRule.getEventId(), DemoActors.DEMO_ADMIN_ID, "PRICE_RULE_CREATE", "PriceRule",
                priceRule.getId(), null, null, "create price rule");
        return toResponse(priceRule);
    }

    @Transactional(readOnly = true)
    public List<PriceRuleResponse> list(UUID eventId) {
        return repository.findByEventIdOrderByUpdatedAtDesc(eventId).stream().map(this::toResponse).toList();
    }

    private PriceRuleResponse toResponse(PriceRule priceRule) {
        return new PriceRuleResponse(
                priceRule.getId(),
                priceRule.getEventId(),
                priceRule.getZoneId(),
                priceRule.getBoothType(),
                priceRule.getUnitPrice(),
                priceRule.isEnabled()
        );
    }
}
