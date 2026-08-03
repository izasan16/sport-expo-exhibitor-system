package com.sportexpo.exhibitor.booth;

import com.sportexpo.exhibitor.booth.dto.BoothResponse;
import com.sportexpo.exhibitor.booth.dto.CreateBoothRequest;
import com.sportexpo.exhibitor.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
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
@RequestMapping("/api/booths")
public class BoothController {
    private final BoothService service;

    public BoothController(BoothService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN','ORGANIZER_ADMIN')")
    public ApiResponse<BoothResponse> create(@Valid @RequestBody CreateBoothRequest request) {
        return ApiResponse.ok(service.create(request));
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN','ORGANIZER_ADMIN','SALES','FINANCE','SERVICE_STAFF','EXHIBITOR')")
    public ApiResponse<List<BoothResponse>> list(@RequestParam UUID eventId,
                                                 @RequestParam(required = false) String status) {
        return ApiResponse.ok(service.list(eventId, status));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN','ORGANIZER_ADMIN','SALES','FINANCE','SERVICE_STAFF','EXHIBITOR')")
    public ApiResponse<BoothResponse> get(@PathVariable UUID id) {
        return ApiResponse.ok(service.get(id));
    }
}
