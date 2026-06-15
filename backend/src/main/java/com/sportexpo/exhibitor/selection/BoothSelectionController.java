package com.sportexpo.exhibitor.selection;

import com.sportexpo.exhibitor.common.ApiResponse;
import com.sportexpo.exhibitor.common.DemoActors;
import com.sportexpo.exhibitor.selection.dto.BoothSelectionResponse;
import com.sportexpo.exhibitor.selection.dto.CreateBoothSelectionRequest;
import com.sportexpo.exhibitor.selection.dto.RejectBoothSelectionRequest;
import com.sportexpo.exhibitor.selection.dto.SelectionEligibilityResponse;
import com.sportexpo.exhibitor.selection.dto.SubmitBoothSelectionRequest;
import jakarta.validation.Valid;
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

    public BoothSelectionController(BoothSelectionService service) {
        this.service = service;
    }

    @GetMapping("/api/selection-eligibilities")
    public ApiResponse<SelectionEligibilityResponse> eligibility(@RequestParam UUID orderId) {
        return ApiResponse.ok(service.getEligibility(orderId));
    }

    @PostMapping("/api/booth-selections")
    public ApiResponse<BoothSelectionResponse> submit(@Valid @RequestBody SubmitBoothSelectionRequest request) {
        var demoRequest = new CreateBoothSelectionRequest(
                request.orderId(),
                DemoActors.DEMO_EXHIBITOR_ID,
                request.boothIds(),
                request.remark()
        );
        return ApiResponse.ok(service.submit(demoRequest));
    }

    @GetMapping("/api/booth-selections")
    public ApiResponse<List<BoothSelectionResponse>> list(@RequestParam(required = false) UUID orderId,
                                                          @RequestParam(required = false) String status) {
        return ApiResponse.ok(service.list(orderId, status));
    }

    @PostMapping("/api/booth-selections/{selectionId}/approve")
    public ApiResponse<BoothSelectionResponse> approve(@PathVariable UUID selectionId) {
        return ApiResponse.ok(service.approve(selectionId, DemoActors.DEMO_ADMIN_ID));
    }

    @PostMapping("/api/booth-selections/{selectionId}/reject")
    public ApiResponse<BoothSelectionResponse> reject(@PathVariable UUID selectionId,
                                                      @Valid @RequestBody RejectBoothSelectionRequest request) {
        return ApiResponse.ok(service.reject(selectionId, request, DemoActors.DEMO_ADMIN_ID));
    }

    @PostMapping("/api/booth-selections/{selectionId}/timeout-release")
    public ApiResponse<BoothSelectionResponse> timeoutRelease(@PathVariable UUID selectionId) {
        return ApiResponse.ok(service.timeoutRelease(selectionId, DemoActors.DEMO_ADMIN_ID));
    }
}
