package com.sportexpo.exhibitor.registration;

import com.sportexpo.exhibitor.common.ApiResponse;
import com.sportexpo.exhibitor.common.DemoActors;
import com.sportexpo.exhibitor.registration.dto.RegistrationRequest;
import com.sportexpo.exhibitor.registration.dto.RegistrationResponse;
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
@RequestMapping("/api/registrations")
public class RegistrationController {
    private final RegistrationService service;

    public RegistrationController(RegistrationService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<RegistrationResponse>> list(@RequestParam(required = false) UUID eventId,
                                                        @RequestParam(required = false) UUID exhibitorId,
                                                        @RequestParam(required = false) String status) {
        return ApiResponse.ok(service.list(eventId, exhibitorId, status));
    }

    @GetMapping("/{id}")
    public ApiResponse<RegistrationResponse> get(@PathVariable UUID id) {
        return ApiResponse.ok(service.get(id));
    }

    @PostMapping
    public ApiResponse<RegistrationResponse> create(@RequestBody RegistrationRequest request) {
        return ApiResponse.ok(service.create(request, DemoActors.DEMO_EXHIBITOR_ID));
    }
}
