package com.emergencyconnectuae.service;

import com.emergencyconnectuae.dto.AssignmentRequest;
import com.emergencyconnectuae.dto.PaginatedResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import com.emergencyconnectuae.model.*;
import com.emergencyconnectuae.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final IncidentRepository incidentRepository;
    private final ResourceRepository resourceRepository;
    private final UserRepository userRepository;
    private final RedisLockService redisLockService;
    private final RedisCacheService redisCacheService;
    private final AuditLogService auditLogService;
    private final DashboardService dashboardService;
    public AssignmentService(AssignmentRepository assignmentRepository,
                             IncidentRepository incidentRepository,
                             ResourceRepository resourceRepository,
                             UserRepository userRepository,
                             RedisLockService redisLockService,
                             RedisCacheService redisCacheService,
                             AuditLogService auditLogService, DashboardService dashboardService) {
        this.assignmentRepository = assignmentRepository;
        this.incidentRepository = incidentRepository;
        this.resourceRepository = resourceRepository;
        this.userRepository = userRepository;
        this.redisLockService = redisLockService;
        this.redisCacheService = redisCacheService;
        this.auditLogService = auditLogService;
        this.dashboardService = dashboardService;
    }

    public Assignment assignResource(AssignmentRequest request, String performedBy) {

        String lockKey = "lock:resource:" + request.getResourceId();
        String lockValue = UUID.randomUUID().toString();

        boolean locked = redisLockService.acquireLock(lockKey, lockValue, 10);

        if (!locked) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Resource is currently being assigned by another dispatcher"
            );
        }

        try {
            Incident incident = incidentRepository.findById(request.getIncidentId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Incident not found"));

            EmergencyResource resource = resourceRepository.findById(request.getResourceId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found"));

            User assignedBy = userRepository.findById(request.getAssignedByUserId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

            if (incident.getStatus() == IncidentStatus.RESOLVED) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Cannot assign resource to resolved incident"
                );
            }

            if (resource.getStatus() != ResourceStatus.AVAILABLE) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Resource is not available"
                );
            }

            resource.setStatus(ResourceStatus.ASSIGNED);
            resourceRepository.save(resource);

            Assignment assignment = Assignment.builder()
                    .incident(incident)
                    .resource(resource)
                    .assignedBy(assignedBy)
                    .assignedAt(LocalDateTime.now())
                    .build();

            Assignment saved = assignmentRepository.save(assignment);

            redisCacheService.delete("available_resources");
            redisCacheService.delete("available_resources:page:0:size:10");

            auditLogService.log(
                    "ASSIGN_RESOURCE",
                    performedBy,
                    "Assignment",
                    saved.getId(),
                    "Resource " + resource.getResourceName() + " assigned to incident " + incident.getTitle()
            );
            dashboardService.clearDashboardCache();

            return saved;

        } finally {
            redisLockService.releaseLock(lockKey, lockValue);
        }
    }
    
    public PaginatedResponse<Assignment> getAssignments(int page, int size) {

        Page<Assignment> assignments = assignmentRepository.findAll(PageRequest.of(page, size));

        return new PaginatedResponse<>(
                assignments.getContent(),
                assignments.getNumber(),
                assignments.getSize(),
                assignments.getTotalElements()
        );
    }
}