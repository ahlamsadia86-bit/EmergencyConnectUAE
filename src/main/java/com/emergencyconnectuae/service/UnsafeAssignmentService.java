package com.emergencyconnectuae.service;

import com.emergencyconnectuae.dto.AssignmentRequest;
import com.emergencyconnectuae.model.*;
import com.emergencyconnectuae.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Service
public class UnsafeAssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final IncidentRepository incidentRepository;
    private final ResourceRepository resourceRepository;
    private final UserRepository userRepository;

    public UnsafeAssignmentService(AssignmentRepository assignmentRepository,
                                   IncidentRepository incidentRepository,
                                   ResourceRepository resourceRepository,
                                   UserRepository userRepository) {
        this.assignmentRepository = assignmentRepository;
        this.incidentRepository = incidentRepository;
        this.resourceRepository = resourceRepository;
        this.userRepository = userRepository;
    }

    public Assignment assignWithoutLock(AssignmentRequest request) {

        Incident incident = incidentRepository.findById(request.getIncidentId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Incident not found"));

        EmergencyResource resource = resourceRepository.findById(request.getResourceId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found"));

        User assignedBy = userRepository.findById(request.getAssignedByUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (resource.getStatus() != ResourceStatus.AVAILABLE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Resource is not available");
        }

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        resource.setStatus(ResourceStatus.ASSIGNED);
        resourceRepository.save(resource);

        Assignment assignment = Assignment.builder()
                .incident(incident)
                .resource(resource)
                .assignedBy(assignedBy)
                .assignedAt(LocalDateTime.now())
                .build();

        return assignmentRepository.save(assignment);
    }
}
