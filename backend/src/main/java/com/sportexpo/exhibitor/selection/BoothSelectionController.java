package com.sportexpo.exhibitor.selection;

import com.sportexpo.exhibitor.common.ApiResponse;
import com.sportexpo.exhibitor.order.ParticipationOrderService;
import com.sportexpo.exhibitor.security.CurrentUserService;
import com.sportexpo.exhibitor.security.DataScopeService;
import com.sportexpo.exhibitor.selection.dto.BoothSelectionResponse;
import com.sportexpo.exhibitor.selection.dto.CreateBoothSelectionRequest;
import com.sportexpo.exhibitor.selection.dto.RejectBoothSelectionRequest;
import com.sportexpo.exhibitor.selection.dto.SelectionEligibilityResponse;
import com.sportexpo.exhibitor.selection.dto.SubmitBoothSelectionRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
public class BoothSelectionController {
    private final BoothSelectionService service;
    private final CurrentUserService currentUserService;
    private final DataScopeService dataScopeService;
    private final ParticipationOrderService orderService;

    public BoothSelectionController(BoothSelectionService service, CurrentUserService currentUserService,
                                    DataScopeService dataScopeService, ParticipationOrderService orderService) {
        this.service = service;
        this.currentUserService = currentUserService;
        this.dataScopeService = dataScopeService;
        this.orderService = orderService;
    }

    @GetMapping("/api/selection-eligibilities")
    @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN','ORGANIZER_ADMIN','SALES','SERVICE_STAFF','EXHIBITOR')")
    public ApiResponse<SelectionEligibilityResponse> eligibility(@RequestParam UUID orderId,
                                                                 Authentication authentication) {
        dataScopeService.requireCanAccessExhibitorIfScoped(authentication, orderService.get(orderId).exhibitorId());
        return ApiResponse.ok(service.getEligibility(orderId));
    }

    @PostMapping("/api/booth-selections")
    @PreAuthorize("hasAuthority('EXHIBITOR')")
    public ApiResponse<BoothSelectionResponse> submit(@Valid @RequestBody SubmitBoothSelectionRequest request,
                                                      Authentication authentication) {
        var scopedRequest = new CreateBoothSelectionRequest(
                request.orderId(),
                currentUserService.requireCurrentExhibitorId(authentication),
                request.boothIds(),
                request.remark()
        );
        return ApiResponse.ok(service.submit(scopedRequest));
    }

    @GetMapping("/api/booth-selections")
    @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN','ORGANIZER_ADMIN','SALES','SERVICE_STAFF','EXHIBITOR')")
    public ApiResponse<List<BoothSelectionResponse>> list(@RequestParam(required = false) UUID orderId,
                                                          @RequestParam(required = false) String status,
                                                          Authentication authentication) {
        var selections = service.list(orderId, status);
        if (!dataScopeService.isExhibitorScoped(authentication)) {
            return ApiResponse.ok(selections);
        }
        var allowedIds = dataScopeService.accessibleExhibitorIds(authentication);
        return ApiResponse.ok(selections.stream()
                .filter(selection -> allowedIds.contains(selection.exhibitorId()))
                .toList());
    }

    @PostMapping("/api/booth-selections/{selectionId}/approve")
    @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN','ORGANIZER_ADMIN','SERVICE_STAFF')")
    public ApiResponse<BoothSelectionResponse> approve(@PathVariable UUID selectionId, Authentication authentication) {
        return ApiResponse.ok(service.approve(selectionId, currentUserService.getCurrentUser(authentication).userId()));
    }

    @PostMapping("/api/booth-selections/{selectionId}/reject")
    @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN','ORGANIZER_ADMIN','SERVICE_STAFF')")
    public ApiResponse<BoothSelectionResponse> reject(@PathVariable UUID selectionId,
                                                      @Valid @RequestBody RejectBoothSelectionRequest request,
                                                      Authentication authentication) {
        return ApiResponse.ok(service.reject(selectionId, request, currentUserService.getCurrentUser(authentication).userId()));
    }

    @PostMapping("/api/booth-selections/{selectionId}/timeout-release")
    @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN','ORGANIZER_ADMIN','SERVICE_STAFF')")
    public ApiResponse<BoothSelectionResponse> timeoutRelease(@PathVariable UUID selectionId,
                                                              Authentication authentication) {
        return ApiResponse.ok(service.timeoutRelease(selectionId, currentUserService.getCurrentUser(authentication).userId()));
    }
}
