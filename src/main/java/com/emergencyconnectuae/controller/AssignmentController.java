package com.emergencyconnectuae.controller;

import com.emergencyconnectuae.dto.AssignmentRequest;
import com.emergencyconnectuae.dto.PaginatedResponse;
import com.emergencyconnectuae.model.Assignment;
import com.emergencyconnectuae.service.AssignmentService;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/assignments")
public class AssignmentController {

    private final AssignmentService assignmentService;

    public AssignmentController(AssignmentService assignmentService) {
        this.assignmentService = assignmentService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('DISPATCHER','ADMIN')")
    public ResponseEntity<Assignment> assignResource(@RequestBody AssignmentRequest request,
                                                     Authentication authentication) {

        Assignment assignment = assignmentService.assignResource(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(assignment);
    }
    
    @GetMapping
    @PreAuthorize("hasAnyRole('DISPATCHER','ADMIN')")
    public ResponseEntity<PaginatedResponse<Assignment>> getAssignments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return ResponseEntity.ok(assignmentService.getAssignments(page, size));
    }
}