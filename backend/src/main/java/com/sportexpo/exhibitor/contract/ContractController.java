package com.sportexpo.exhibitor.contract;

import com.sportexpo.exhibitor.common.ApiResponse;
import com.sportexpo.exhibitor.contract.dto.ContractResponse;
import com.sportexpo.exhibitor.contract.dto.ContractVersionResponse;
import com.sportexpo.exhibitor.security.CurrentUserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
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
    private final CurrentUserService currentUserService;

    public ContractController(ContractService service, CurrentUserService currentUserService) {
        this.service = service;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN','ORGANIZER_ADMIN','SALES','FINANCE','EXHIBITOR')")
    public ApiResponse<List<ContractResponse>> list(@RequestParam(required = false) UUID orderId) {
        return ApiResponse.ok(service.list(orderId));
    }

    @GetMapping("/{contractId}/versions")
    @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN','ORGANIZER_ADMIN','SALES','FINANCE','EXHIBITOR')")
    public ApiResponse<List<ContractVersionResponse>> versions(@PathVariable UUID contractId) {
        return ApiResponse.ok(service.listVersions(contractId));
    }

    @PostMapping("/{contractId}/mark-offline-signed")
    @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN','ORGANIZER_ADMIN','SALES')")
    public ApiResponse<ContractResponse> markOfflineSigned(@PathVariable UUID contractId,
                                                           Authentication authentication) {
        return ApiResponse.ok(service.markOfflineSigned(contractId, currentUserService.getCurrentUser(authentication).userId()));
    }
}
