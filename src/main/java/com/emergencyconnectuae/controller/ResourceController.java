package com.emergencyconnectuae.controller;

import com.emergencyconnectuae.dto.*;
import com.emergencyconnectuae.model.EmergencyResource;
import com.emergencyconnectuae.service.ResourceService;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/resources")
public class ResourceController {

    private final ResourceService resourceService;

    public ResourceController(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('DISPATCHER','HOSPITAL_ADMIN','ADMIN')")
    public ResponseEntity<EmergencyResource> createResource(@Valid @RequestBody ResourceRequest request,
                                                            Authentication authentication) {
        EmergencyResource resource = resourceService.createResource(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(resource);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('DISPATCHER','RESPONDER','HOSPITAL_ADMIN','ADMIN')")
    public ResponseEntity<PaginatedResponse<EmergencyResource>> getAllResources(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return ResponseEntity.ok(resourceService.getAllResources(page, size));
    }

    @GetMapping("/available")
    @PreAuthorize("hasAnyRole('DISPATCHER','RESPONDER','HOSPITAL_ADMIN','ADMIN')")
    public ResponseEntity<PaginatedResponse<EmergencyResource>> getAvailableResources(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return ResponseEntity.ok(resourceService.getAvailableResources(page, size));
    }

    @PutMapping("/{id}/location")
    @PreAuthorize("hasAnyRole('RESPONDER','DISPATCHER','HOSPITAL_ADMIN','ADMIN')")
    public ResponseEntity<EmergencyResource> updateLiveLocation(
            @PathVariable Long id,
            @RequestBody LocationUpdateRequest request,
            Authentication authentication) {

        return ResponseEntity.ok(
                resourceService.updateLiveLocation(id, request, authentication.getName())
        );
    }
    @PutMapping("/{id}/availability")
    @PreAuthorize("hasAnyRole('DISPATCHER','HOSPITAL_ADMIN','ADMIN')")
    public ResponseEntity<EmergencyResource> updateAvailability(
            @PathVariable Long id,
            @RequestBody ResourceStatusUpdateRequest request,
            Authentication authentication) {

        return ResponseEntity.ok(
                resourceService.updateAvailability(id, request.getStatus(), authentication.getName())
        );
    }
}