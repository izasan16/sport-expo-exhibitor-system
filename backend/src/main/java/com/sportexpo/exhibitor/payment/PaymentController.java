package com.sportexpo.exhibitor.payment;

import com.sportexpo.exhibitor.common.ApiResponse;
import com.sportexpo.exhibitor.payment.dto.CreatePaymentRequest;
import com.sportexpo.exhibitor.payment.dto.FailPaymentRequest;
import com.sportexpo.exhibitor.payment.dto.PaymentResponse;
import com.sportexpo.exhibitor.order.ParticipationOrderService;
import com.sportexpo.exhibitor.security.CurrentUserService;
import com.sportexpo.exhibitor.security.DataScopeService;
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
@RequestMapping("/api/payments")
public class PaymentController {
    private final PaymentService service;
    private final CurrentUserService currentUserService;
    private final DataScopeService dataScopeService;
    private final ParticipationOrderService orderService;

    public PaymentController(PaymentService service, CurrentUserService currentUserService,
                             DataScopeService dataScopeService, ParticipationOrderService orderService) {
        this.service = service;
        this.currentUserService = currentUserService;
        this.dataScopeService = dataScopeService;
        this.orderService = orderService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('EXHIBITOR')")
    public ApiResponse<PaymentResponse> create(@Valid @RequestBody CreatePaymentRequest request,
                                               Authentication authentication) {
        var currentUser = currentUserService.getCurrentUser(authentication);
        return ApiResponse.ok(service.create(request, currentUser.userId(),
                currentUserService.requireCurrentExhibitorId(authentication)));
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN','ORGANIZER_ADMIN','FINANCE','EXHIBITOR')")
    public ApiResponse<List<PaymentResponse>> list(@RequestParam(required = false) UUID orderId,
                                                   @RequestParam(required = false) String status,
                                                   Authentication authentication) {
        var payments = service.list(orderId, status);
        if (!dataScopeService.isExhibitorScoped(authentication)) {
            return ApiResponse.ok(payments);
        }
        var allowedIds = dataScopeService.accessibleExhibitorIds(authentication);
        return ApiResponse.ok(payments.stream()
                .filter(payment -> allowedIds.contains(orderService.get(payment.orderId()).exhibitorId()))
                .toList());
    }

    @PostMapping("/{paymentId}/confirm")
    @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN','ORGANIZER_ADMIN','FINANCE')")
    public ApiResponse<PaymentResponse> confirm(@PathVariable UUID paymentId, Authentication authentication) {
        return ApiResponse.ok(service.confirm(paymentId, currentUserService.getCurrentUser(authentication).userId()));
    }

    @PostMapping("/{paymentId}/fail")
    @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN','ORGANIZER_ADMIN','FINANCE')")
    public ApiResponse<PaymentResponse> fail(@PathVariable UUID paymentId,
                                             @Valid @RequestBody FailPaymentRequest request,
                                             Authentication authentication) {
        return ApiResponse.ok(service.fail(paymentId, currentUserService.getCurrentUser(authentication).userId(), request.reason()));
    }
}
