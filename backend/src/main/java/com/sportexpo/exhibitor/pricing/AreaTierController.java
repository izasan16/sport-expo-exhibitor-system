package com.sportexpo.exhibitor.pricing;

import com.sportexpo.exhibitor.common.ApiResponse;
import com.sportexpo.exhibitor.pricing.dto.AreaTierResponse;
import com.sportexpo.exhibitor.pricing.dto.CreateAreaTierRequest;
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
@RequestMapping("/api/area-tiers")
public class AreaTierController {
    private final AreaTierService service;

    public AreaTierController(AreaTierService service) {
        this.service = service;
    }

    @PostMapping
    public ApiResponse<AreaTierResponse> create(@Valid @RequestBody CreateAreaTierRequest request) {
        return ApiResponse.ok(service.create(request));
    }

    @GetMapping
    public ApiResponse<List<AreaTierResponse>> list(@RequestParam UUID eventId) {
        return ApiResponse.ok(service.list(eventId));
    }
}
