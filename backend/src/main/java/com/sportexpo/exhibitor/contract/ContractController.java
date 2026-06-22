package com.sportexpo.exhibitor.contract;

import com.sportexpo.exhibitor.common.ApiResponse;
import com.sportexpo.exhibitor.contract.dto.ContractResponse;
import com.sportexpo.exhibitor.contract.dto.ContractVersionResponse;
import com.sportexpo.exhibitor.order.ParticipationOrderService;
import com.sportexpo.exhibitor.security.CurrentUserService;
import com.sportexpo.exhibitor.security.DataScopeService;
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
    private final DataScopeService dataScopeService;
    private final ParticipationOrderService orderService;

    public ContractController(ContractService service, CurrentUserService currentUserService,
                              DataScopeService dataScopeService, ParticipationOrderService orderService) {
        this.service = service;
        this.currentUserService = currentUserService;
        this.dataScopeService = dataScopeService;
        this.orderService = orderService;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN','ORGANIZER_ADMIN','SALES','FINANCE','EXHIBITOR')")
    public ApiResponse<List<ContractResponse>> list(@RequestParam(required = false) UUID orderId,
                                                    Authentication authentication) {
        if (!dataScopeService.isExhibitorScoped(authentication)) {
            return ApiResponse.ok(service.list(orderId));
        }
        return ApiResponse.ok(service.listScoped(orderId,
                dataScopeService.accessibleExhibitorIds(authentication)));
    }

    @GetMapping("/{contractId}/versions")
    @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN','ORGANIZER_ADMIN','SALES','FINANCE','EXHIBITOR')")
    public ApiResponse<List<ContractVersionResponse>> versions(@PathVariable UUID contractId,
                                                               Authentication authentication) {
        requireCanAccessContract(authentication, contractId);
        return ApiResponse.ok(service.listVersions(contractId));
    }

    @PostMapping("/{contractId}/mark-offline-signed")
    @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN','ORGANIZER_ADMIN','SALES')")
    public ApiResponse<ContractResponse> markOfflineSigned(@PathVariable UUID contractId,
                                                           Authentication authentication) {
        requireCanAccessContract(authentication, contractId);
        return ApiResponse.ok(service.markOfflineSigned(contractId, currentUserService.getCurrentUser(authentication).userId()));
    }

    private void requireCanAccessContract(Authentication authentication, UUID contractId) {
        var contract = service.get(contractId);
        var order = orderService.get(contract.orderId());
        dataScopeService.requireCanAccessExhibitorIfScoped(authentication, order.exhibitorId());
    }
}
