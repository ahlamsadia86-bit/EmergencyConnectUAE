package com.emergencyconnectuae.controller;

import com.emergencyconnectuae.dto.*;
import com.emergencyconnectuae.service.DispatchService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.emergencyconnectuae.dto.AutoDispatchRequest;
import com.emergencyconnectuae.dto.AutoDispatchResponse;
@RestController
@RequestMapping("/api/dispatch")
public class DispatchController {

    private final DispatchService dispatchService;

    public DispatchController(DispatchService dispatchService) {
        this.dispatchService = dispatchService;
    }

    @PostMapping("/suggest")
    @PreAuthorize("hasAnyRole('DISPATCHER','ADMIN')")
    public ResponseEntity<DispatchSuggestionResponse> suggestBestResource(
            @RequestBody DispatchSuggestionRequest request,
            Authentication authentication) {

        return ResponseEntity.ok(
                dispatchService.suggestBestResource(request.getIncidentId(), authentication.getName())
        );
    }
    
    @PostMapping("/auto")
    @PreAuthorize("hasAnyRole('DISPATCHER','ADMIN')")
    public ResponseEntity<AutoDispatchResponse> autoDispatch(
            @RequestBody AutoDispatchRequest request,
            Authentication authentication) {

        return ResponseEntity.ok(
                dispatchService.autoDispatch(
                        request.getIncidentId(),
                        request.getAssignedByUserId(),
                        authentication.getName()
                )
        );
    }
}
