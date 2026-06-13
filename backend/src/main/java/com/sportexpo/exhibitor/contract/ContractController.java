package com.sportexpo.exhibitor.contract;

import com.sportexpo.exhibitor.common.ApiResponse;
import com.sportexpo.exhibitor.common.DemoActors;
import com.sportexpo.exhibitor.contract.dto.ContractResponse;
import com.sportexpo.exhibitor.contract.dto.ContractVersionResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/contracts")
public class ContractController {
    private final ContractService service;

    public ContractController(ContractService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<ContractResponse>> list(@RequestParam(required = false) UUID orderId) {
        return ApiResponse.ok(service.list(orderId));
    }

    @GetMapping("/{contractId}/versions")
    public ApiResponse<List<ContractVersionResponse>> versions(@PathVariable UUID contractId) {
        return ApiResponse.ok(service.listVersions(contractId));
    }

    @PostMapping("/{contractId}/mark-offline-signed")
    public ApiResponse<ContractResponse> markOfflineSigned(@PathVariable UUID contractId) {
        return ApiResponse.ok(service.markOfflineSigned(contractId, DemoActors.DEMO_ADMIN_ID));
    }
}
