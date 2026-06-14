package com.sportexpo.exhibitor.order;

import com.sportexpo.exhibitor.common.ApiResponse;
import com.sportexpo.exhibitor.common.DemoActors;
import com.sportexpo.exhibitor.order.dto.BatchConfirmOrdersRequest;
import com.sportexpo.exhibitor.order.dto.BatchConfirmOrdersResponse;
import com.sportexpo.exhibitor.order.dto.CreateParticipationOrderRequest;
import com.sportexpo.exhibitor.order.dto.ParticipationOrderResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/participation-orders")
public class ParticipationOrderController {
    private final ParticipationOrderService service;

    public ParticipationOrderController(ParticipationOrderService service) {
        this.service = service;
    }

    @PostMapping
    public ApiResponse<ParticipationOrderResponse> create(@Valid @RequestBody CreateParticipationOrderRequest request) {
        return ApiResponse.ok(service.create(request));
    }

    @PostMapping("/{orderId}/confirm")
    public ApiResponse<ParticipationOrderResponse> confirm(@PathVariable UUID orderId) {
        return ApiResponse.ok(service.confirmSales(orderId, DemoActors.DEMO_ADMIN_ID));
    }

    @PostMapping("/batch-confirm")
    public ApiResponse<BatchConfirmOrdersResponse> batchConfirm(@Valid @RequestBody BatchConfirmOrdersRequest request) {
        return ApiResponse.ok(service.batchConfirmSales(request.orderIds(), DemoActors.DEMO_ADMIN_ID));
    }

    @GetMapping
    public ApiResponse<List<ParticipationOrderResponse>> list(@RequestParam(required = false) UUID eventId,
                                                              @RequestParam(required = false) UUID exhibitorId,
                                                              @RequestParam(required = false) String status) {
        return ApiResponse.ok(service.list(eventId, exhibitorId, status));
    }

    @GetMapping("/{orderId}")
    public ApiResponse<ParticipationOrderResponse> get(@PathVariable UUID orderId) {
        return ApiResponse.ok(service.get(orderId));
    }
}
