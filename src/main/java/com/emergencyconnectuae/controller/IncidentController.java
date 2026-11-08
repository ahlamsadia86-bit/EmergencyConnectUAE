package com.emergencyconnectuae.controller;

import com.emergencyconnectuae.dto.*;
import com.emergencyconnectuae.model.Incident;
import com.emergencyconnectuae.service.IncidentService;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/incidents")
public class IncidentController {

    private final IncidentService incidentService;

    public IncidentController(IncidentService incidentService) {
        this.incidentService = incidentService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('DISPATCHER','ADMIN')")
    public ResponseEntity<Incident> createIncident(@Valid @RequestBody IncidentRequest request,
                                                   Authentication authentication) {
        Incident incident = incidentService.createIncident(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(incident);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('DISPATCHER','RESPONDER','ADMIN')")
    public ResponseEntity<PaginatedResponse<Incident>> getAllIncidents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return ResponseEntity.ok(incidentService.getAllIncidents(page, size));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('DISPATCHER','RESPONDER','ADMIN')")
    public ResponseEntity<Incident> getIncidentById(@PathVariable Long id) {
        return ResponseEntity.ok(incidentService.getIncidentById(id));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('DISPATCHER','RESPONDER','ADMIN')")
    public ResponseEntity<Incident> updateStatus(@PathVariable Long id,
                                                 @RequestBody StatusUpdateRequest request,
                                                 Authentication authentication) {

        return ResponseEntity.ok(
                incidentService.updateStatus(id, request, authentication.getName())
        );
    }
}