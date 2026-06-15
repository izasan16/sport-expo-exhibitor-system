package com.sportexpo.exhibitor.exhibitor;

import com.sportexpo.exhibitor.common.ApiResponse;
import com.sportexpo.exhibitor.common.DemoActors;
import com.sportexpo.exhibitor.exhibitor.dto.ExhibitorRequest;
import com.sportexpo.exhibitor.exhibitor.dto.ExhibitorResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/exhibitors")
public class ExhibitorController {
    private final ExhibitorService service;

    public ExhibitorController(ExhibitorService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<ExhibitorResponse>> list() {
        return ApiResponse.ok(service.list());
    }

    @GetMapping("/current")
    public ApiResponse<ExhibitorResponse> current() {
        return ApiResponse.ok(service.get(DemoActors.DEMO_EXHIBITOR_ID));
    }

    @GetMapping("/{id}")
    public ApiResponse<ExhibitorResponse> get(@PathVariable UUID id) {
        return ApiResponse.ok(service.get(id));
    }

    @PostMapping
    public ApiResponse<ExhibitorResponse> create(@RequestBody ExhibitorRequest request) {
        return ApiResponse.ok(service.create(request, DemoActors.DEMO_ADMIN_ID));
    }

    @PutMapping("/{id}")
    public ApiResponse<ExhibitorResponse> update(@PathVariable UUID id, @RequestBody ExhibitorRequest request) {
        return ApiResponse.ok(service.update(id, request, DemoActors.DEMO_ADMIN_ID));
    }

    @PutMapping("/current")
    public ApiResponse<ExhibitorResponse> updateCurrent(@RequestBody ExhibitorRequest request) {
        return ApiResponse.ok(service.update(DemoActors.DEMO_EXHIBITOR_ID, request, DemoActors.DEMO_EXHIBITOR_ID));
    }
}
