package com.sportexpo.exhibitor.pricing;

import com.sportexpo.exhibitor.common.ApiResponse;
import com.sportexpo.exhibitor.pricing.dto.CreatePriceRuleRequest;
import com.sportexpo.exhibitor.pricing.dto.PriceRuleResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/price-rules")
public class PriceRuleController {
    private final PriceRuleService service;

    public PriceRuleController(PriceRuleService service) {
        this.service = service;
    }

    @PostMapping
    public ApiResponse<PriceRuleResponse> create(@Valid @RequestBody CreatePriceRuleRequest request) {
        return ApiResponse.ok(service.create(request));
    }

    @GetMapping
    public ApiResponse<List<PriceRuleResponse>> list(@RequestParam UUID eventId) {
        return ApiResponse.ok(service.list(eventId));
    }
}
