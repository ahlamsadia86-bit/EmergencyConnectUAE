package com.emergencyconnectuae.service;

import com.emergencyconnectuae.dto.*;
import com.emergencyconnectuae.model.*;
import com.emergencyconnectuae.repository.IncidentRepository;
import com.emergencyconnectuae.repository.UserRepository;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Service
public class IncidentService {

    private final DashboardService dashboardService;
    private final IncidentRepository incidentRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    public IncidentService(IncidentRepository incidentRepository,
                           UserRepository userRepository,
                           AuditLogService auditLogService,
                           DashboardService dashboardService) {
        this.incidentRepository = incidentRepository;
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
        this.dashboardService = dashboardService;
    }

    public Incident createIncident(IncidentRequest request, String performedBy) {

        User reporter = userRepository.findByEmail(performedBy)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        Incident incident = Incident.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .location(request.getLocation())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .region(resolveRegion(request.getRegion(), request.getLocation()))
                .requiredResourceType(request.getRequiredResourceType() == null ? ResourceType.AMBULANCE : request.getRequiredResourceType())
                .priority(request.getPriority() == null ? IncidentPriority.MEDIUM : request.getPriority())
                .status(IncidentStatus.OPEN)
                .reportedBy(reporter)
                .reportedAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Incident saved = incidentRepository.save(incident);

        auditLogService.log(
                "CREATE_INCIDENT",
                performedBy,
                "Incident",
                saved.getId(),
                "New incident reported by " + reporter.getEmail() + ": " + saved.getTitle()
        );

        dashboardService.clearDashboardCache();
        dashboardService.clearActiveIncidentsCache();

        return saved;
    }

    public PaginatedResponse<Incident> getAllIncidents(int page, int size) {

        Page<Incident> incidents = incidentRepository.findAll(PageRequest.of(page, size));

        return new PaginatedResponse<>(
                incidents.getContent(),
                incidents.getNumber(),
                incidents.getSize(),
                incidents.getTotalElements()
        );
    }

    public Incident getIncidentById(Long id) {
        return incidentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Incident not found"));
    }

    public Incident updateStatus(Long id, StatusUpdateRequest request, String performedBy) {

        Incident incident = getIncidentById(id);

        if (request.getStatus() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status is required");
        }

        User currentUser = userRepository.findByEmail(performedBy)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        enforceIncidentUpdatePermission(incident, currentUser);

        IncidentStatus oldStatus = incident.getStatus();
        IncidentStatus newStatus = request.getStatus();

        validateStatusTransition(oldStatus, newStatus);

        incident.setStatus(newStatus);
        incident.setUpdatedAt(LocalDateTime.now());

        Incident saved = incidentRepository.save(incident);

        auditLogService.log(
                "UPDATE_INCIDENT_STATUS",
                performedBy,
                "Incident",
                saved.getId(),
                "Status changed from " + oldStatus + " to " + newStatus
        );

        dashboardService.clearDashboardCache();
        dashboardService.clearActiveIncidentsCache();

        return saved;
    }

    private void enforceIncidentUpdatePermission(Incident incident, User currentUser) {

        System.out.println("DEBUG UPDATE INCIDENT:");
        System.out.println("Logged user email = " + currentUser.getEmail());
        System.out.println("Logged user role = " + currentUser.getRole());

        if (currentUser.getRole() == Role.ADMIN) {
            System.out.println("Permission granted: ADMIN");
            return;
        }

        boolean isReporter = incident.getReportedBy() != null
                && incident.getReportedBy().getEmail().equalsIgnoreCase(currentUser.getEmail());

        if (currentUser.getRole() == Role.DISPATCHER && isReporter) {
            System.out.println("Permission granted: reporting dispatcher");
            return;
        }

        if (currentUser.getRole() == Role.RESPONDER) {
            System.out.println("Permission granted: responder");
            return;
        }

        throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "You are not allowed to update this incident. Only the reporting dispatcher, responder, or admin can update it."
        );
    }

    private void validateStatusTransition(IncidentStatus oldStatus, IncidentStatus newStatus) {

        if (oldStatus == IncidentStatus.RESOLVED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Resolved incidents cannot be updated");
        }

        if (oldStatus == IncidentStatus.OPEN && newStatus == IncidentStatus.RESOLVED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Incident must be IN_PROGRESS before RESOLVED");
        }
    }
    
    private Region resolveRegion(Region providedRegion, String location) {

        if (providedRegion != null) {
            return providedRegion;
        }

        if (location == null) {
            return Region.UNKNOWN;
        }

        String loc = location.toLowerCase();

        if (loc.contains("dubai")) {
            return Region.DUBAI;
        }

        if (loc.contains("abu dhabi") || loc.contains("khalifa city") || loc.contains("yas") || loc.contains("mussafah")) {
            return Region.ABU_DHABI;
        }

        if (loc.contains("al ain")) {
            return Region.AL_AIN;
        }

        if (loc.contains("sharjah")) {
            return Region.SHARJAH;
        }

        if (loc.contains("ras al khaimah") || loc.contains("ajman") || loc.contains("umm al quwain") || loc.contains("fujairah")) {
            return Region.NORTHERN_EMIRATES;
        }

        if (loc.contains("liwa") || loc.contains("ruwais") || loc.contains("western")) {
            return Region.WESTERN_REGION;
        }

        return Region.UNKNOWN;
    }
}