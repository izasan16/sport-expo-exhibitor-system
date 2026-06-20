package com.sportexpo.exhibitor.payment;

import com.sportexpo.exhibitor.common.ApiResponse;
import com.sportexpo.exhibitor.payment.dto.CreatePaymentRequest;
import com.sportexpo.exhibitor.payment.dto.FailPaymentRequest;
import com.sportexpo.exhibitor.payment.dto.PaymentResponse;
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
@RequestMapping("/api/payments")
public class PaymentController {
    private final PaymentService service;
    private final CurrentUserService currentUserService;

    public PaymentController(PaymentService service, CurrentUserService currentUserService) {
        this.service = service;
        this.currentUserService = currentUserService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('EXHIBITOR')")
    public ApiResponse<PaymentResponse> create(@Valid @RequestBody CreatePaymentRequest request,
                                               Authentication authentication) {
        return ApiResponse.ok(service.create(request, currentUserService.getCurrentUser(authentication).userId()));
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN','ORGANIZER_ADMIN','FINANCE','EXHIBITOR')")
    public ApiResponse<List<PaymentResponse>> list(@RequestParam(required = false) UUID orderId,
                                                   @RequestParam(required = false) String status) {
        return ApiResponse.ok(service.list(orderId, status));
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
