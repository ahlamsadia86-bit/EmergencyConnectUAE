package com.emergencyconnectuae.controller;

import com.emergencyconnectuae.service.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('DISPATCHER','RESPONDER','HOSPITAL_ADMIN','ADMIN')")
    public ResponseEntity<Map<String, Object>> getSummary() {
        return ResponseEntity.ok(dashboardService.getDashboardSummary());
    }
    
    @GetMapping("/active-incidents")
    @PreAuthorize("hasAnyRole('DISPATCHER','RESPONDER','HOSPITAL_ADMIN','ADMIN')")
    public ResponseEntity<Object> getActiveIncidents() {
        return ResponseEntity.ok(dashboardService.getActiveIncidents());
    }
}