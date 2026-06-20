package com.sportexpo.exhibitor.order;

import com.sportexpo.exhibitor.common.ApiResponse;
import com.sportexpo.exhibitor.order.dto.BatchConfirmOrdersRequest;
import com.sportexpo.exhibitor.order.dto.BatchConfirmOrdersResponse;
import com.sportexpo.exhibitor.order.dto.CreateParticipationOrderRequest;
import com.sportexpo.exhibitor.order.dto.ParticipationOrderResponse;
import com.sportexpo.exhibitor.security.CurrentUserService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
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
    private final CurrentUserService currentUserService;

    public ParticipationOrderController(ParticipationOrderService service, CurrentUserService currentUserService) {
        this.service = service;
        this.currentUserService = currentUserService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('EXHIBITOR')")
    public ApiResponse<ParticipationOrderResponse> create(@Valid @RequestBody CreateParticipationOrderRequest request,
                                                          Authentication authentication) {
        var scopedRequest = new CreateParticipationOrderRequest(
                request.eventId(),
                currentUserService.requireCurrentExhibitorId(authentication),
                request.registrationId(),
                request.zoneId(),
                request.areaTierId(),
                request.boothType()
        );
        return ApiResponse.ok(service.create(scopedRequest));
    }

    @PostMapping("/{orderId}/confirm")
    @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN','ORGANIZER_ADMIN','SALES')")
    public ApiResponse<ParticipationOrderResponse> confirm(@PathVariable UUID orderId, Authentication authentication) {
        return ApiResponse.ok(service.confirmSales(orderId, currentUserService.getCurrentUser(authentication).userId()));
    }

    @PostMapping("/batch-confirm")
    @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN','ORGANIZER_ADMIN','SALES')")
    public ApiResponse<BatchConfirmOrdersResponse> batchConfirm(@Valid @RequestBody BatchConfirmOrdersRequest request,
                                                                Authentication authentication) {
        return ApiResponse.ok(service.batchConfirmSales(request.orderIds(), currentUserService.getCurrentUser(authentication).userId()));
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN','ORGANIZER_ADMIN','SALES','FINANCE','SERVICE_STAFF','EXHIBITOR')")
    public ApiResponse<List<ParticipationOrderResponse>> list(@RequestParam(required = false) UUID eventId,
                                                              @RequestParam(required = false) UUID exhibitorId,
                                                              @RequestParam(required = false) String status) {
        return ApiResponse.ok(service.list(eventId, exhibitorId, status));
    }

    @GetMapping("/{orderId}")
    @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN','ORGANIZER_ADMIN','SALES','FINANCE','SERVICE_STAFF','EXHIBITOR')")
    public ApiResponse<ParticipationOrderResponse> get(@PathVariable UUID orderId) {
        return ApiResponse.ok(service.get(orderId));
    }
}
