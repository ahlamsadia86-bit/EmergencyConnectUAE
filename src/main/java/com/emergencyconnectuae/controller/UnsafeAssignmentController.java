package com.emergencyconnectuae.controller;

import com.emergencyconnectuae.dto.AssignmentRequest;
import com.emergencyconnectuae.model.Assignment;
import com.emergencyconnectuae.service.UnsafeAssignmentService;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/unsafe-demo")
public class UnsafeAssignmentController {

    private final UnsafeAssignmentService unsafeAssignmentService;

    public UnsafeAssignmentController(UnsafeAssignmentService unsafeAssignmentService) {
        this.unsafeAssignmentService = unsafeAssignmentService;
    }

    @PostMapping("/assign-without-lock")
    @PreAuthorize("hasAnyRole('DISPATCHER','ADMIN')")
    public ResponseEntity<Assignment> assignWithoutLock(@RequestBody AssignmentRequest request) {

        Assignment assignment = unsafeAssignmentService.assignWithoutLock(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(assignment);
    }
}
