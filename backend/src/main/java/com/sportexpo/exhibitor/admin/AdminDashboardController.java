package com.sportexpo.exhibitor.admin;

import com.sportexpo.exhibitor.admin.dto.AdminDashboardResponse;
import com.sportexpo.exhibitor.common.ApiResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminDashboardController {
    private final AdminDashboardService service;

    public AdminDashboardController(AdminDashboardService service) {
        this.service = service;
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN','ORGANIZER_ADMIN','SALES','FINANCE','SERVICE_STAFF')")
    public ApiResponse<AdminDashboardResponse> getDashboard() {
        return ApiResponse.ok(service.getDashboard());
    }
}
