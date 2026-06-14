package com.sportexpo.exhibitor.payment;

import com.sportexpo.exhibitor.common.ApiResponse;
import com.sportexpo.exhibitor.common.DemoActors;
import com.sportexpo.exhibitor.payment.dto.CreatePaymentRequest;
import com.sportexpo.exhibitor.payment.dto.FailPaymentRequest;
import com.sportexpo.exhibitor.payment.dto.PaymentResponse;
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
@RequestMapping("/api/payments")
public class PaymentController {
    private final PaymentService service;

    public PaymentController(PaymentService service) {
        this.service = service;
    }

    @PostMapping
    public ApiResponse<PaymentResponse> create(@Valid @RequestBody CreatePaymentRequest request) {
        return ApiResponse.ok(service.create(request));
    }

    @GetMapping
    public ApiResponse<List<PaymentResponse>> list(@RequestParam(required = false) UUID orderId,
                                                   @RequestParam(required = false) String status) {
        return ApiResponse.ok(service.list(orderId, status));
    }

    @PostMapping("/{paymentId}/confirm")
    public ApiResponse<PaymentResponse> confirm(@PathVariable UUID paymentId) {
        return ApiResponse.ok(service.confirm(paymentId, DemoActors.DEMO_ADMIN_ID));
    }

    @PostMapping("/{paymentId}/fail")
    public ApiResponse<PaymentResponse> fail(@PathVariable UUID paymentId,
                                             @Valid @RequestBody FailPaymentRequest request) {
        return ApiResponse.ok(service.fail(paymentId, DemoActors.DEMO_ADMIN_ID, request.reason()));
    }
}
