
package com.emergencyconnectuae.service;

import com.emergencyconnectuae.dto.DispatchSuggestionResponse;
import com.emergencyconnectuae.model.*;
import com.emergencyconnectuae.repository.IncidentRepository;
import com.emergencyconnectuae.repository.ResourceRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;
import com.emergencyconnectuae.dto.AssignmentRequest;
import com.emergencyconnectuae.dto.AutoDispatchResponse;
@Service
public class DispatchService {

    private static final double AVERAGE_EMERGENCY_SPEED_KMH = 60.0;

    private final IncidentRepository incidentRepository;
    private final ResourceRepository resourceRepository;
    private final AuditLogService auditLogService;
    private final AssignmentService assignmentService;

    public DispatchService(IncidentRepository incidentRepository,
            ResourceRepository resourceRepository,
            AuditLogService auditLogService,
            AssignmentService assignmentService) {
this.incidentRepository = incidentRepository;
this.resourceRepository = resourceRepository;
this.auditLogService = auditLogService;
this.assignmentService = assignmentService;
}
    public DispatchSuggestionResponse suggestBestResource(Long incidentId, String performedBy) {

        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Incident not found"));

        if (incident.getStatus() == IncidentStatus.RESOLVED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot dispatch resource to resolved incident");
        }

        List<EmergencyResource> candidates =
                resourceRepository.findByStatus(ResourceStatus.AVAILABLE, PageRequest.of(0, 200))
                        .getContent()
                        .stream()
                        .filter(r -> r.getType() == incident.getRequiredResourceType())
                        .toList();

        if (candidates.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "No available resource matching required type: " + incident.getRequiredResourceType()
            );
        }

        EmergencyResource best = candidates.stream()
                .min(Comparator.comparingDouble(r -> distanceKm(
                        incident.getLatitude(),
                        incident.getLongitude(),
                        r.getLatitude(),
                        r.getLongitude()
                )))
                .orElseThrow();

        double distance = distanceKm(
                incident.getLatitude(),
                incident.getLongitude(),
                best.getLatitude(),
                best.getLongitude()
        );

        double estimatedMinutes = (distance / AVERAGE_EMERGENCY_SPEED_KMH) * 60.0;

        double slaLimit = slaLimitMinutes(incident.getPriority());
        boolean slaMet = estimatedMinutes <= slaLimit;

        String slaLevel = slaText(incident.getPriority(), slaLimit);

        String escalationStatus = slaMet
                ? "NO_ESCALATION_REQUIRED"
                : "ESCALATE_TO_REGIONAL_CONTROL_CENTER";

        String reason = "Selected nearest available " + best.getType()
                + " using coordinates. Distance = "
                + String.format("%.2f", distance)
                + " km, estimated response time = "
                + String.format("%.2f", estimatedMinutes)
                + " minutes. SLA met = " + slaMet + ".";

        auditLogService.log(
                "BONUS_GEO_SLA_DISPATCH",
                performedBy,
                "Incident",
                incident.getId(),
                "Suggested " + best.getResourceName()
                        + " for " + incident.getTitle()
                        + ". Distance: " + String.format("%.2f", distance)
                        + " km. SLA met: " + slaMet
        );

        return new DispatchSuggestionResponse(
                incident.getId(),
                incident.getTitle(),
                incident.getPriority().name(),
                incident.getRequiredResourceType().name(),
                incident.getLocation(),
                best.getId(),
                best.getResourceName(),
                best.getType().name(),
                best.getLocation(),
                round(distance),
                round(estimatedMinutes),
                slaLevel,
                slaMet,
                escalationStatus,
                reason
        );
    }

    private double slaLimitMinutes(IncidentPriority priority) {
        return switch (priority) {
            case CRITICAL -> 5.0;
            case HIGH -> 15.0;
            case MEDIUM -> 30.0;
            case LOW -> 60.0;
        };
    }

    public AutoDispatchResponse autoDispatch(Long incidentId, Long assignedByUserId, String performedBy) {

        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Incident not found"));

        if (incident.getStatus() == IncidentStatus.RESOLVED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot dispatch resolved incident");
        }

        List<EmergencyResource> available = resourceRepository
                .findByStatus(ResourceStatus.AVAILABLE, PageRequest.of(0, 500))
                .getContent()
                .stream()
                .filter(r -> r.getType() == incident.getRequiredResourceType())
                .toList();

        if (available.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No available matching resources");
        }

        EmergencyResource best = available.stream()
                .min(Comparator
                        .comparingInt((EmergencyResource r) -> regionPenalty(incident, r))
                        .thenComparingDouble(r -> distanceKm(
                                incident.getLatitude(),
                                incident.getLongitude(),
                                r.getLatitude(),
                                r.getLongitude()
                        )))
                .orElseThrow();

        double distance = distanceKm(
                incident.getLatitude(),
                incident.getLongitude(),
                best.getLatitude(),
                best.getLongitude()
        );

        double estimatedMinutes = (distance / AVERAGE_EMERGENCY_SPEED_KMH) * 60.0;
        double slaLimit = slaLimitMinutes(incident.getPriority());
        boolean slaMet = estimatedMinutes <= slaLimit;

        String escalationStatus = slaMet
                ? "NO_ESCALATION_REQUIRED"
                : "ESCALATE_TO_REGIONAL_CONTROL_CENTER";

        AssignmentRequest assignmentRequest = new AssignmentRequest();
        assignmentRequest.setIncidentId(incident.getId());
        assignmentRequest.setResourceId(best.getId());
        assignmentRequest.setAssignedByUserId(assignedByUserId);

        assignmentService.assignResource(assignmentRequest, performedBy);

        auditLogService.log(
                "BONUS_AUTOMATED_DISPATCH",
                performedBy,
                "Incident",
                incident.getId(),
                "Automatically dispatched " + best.getResourceName()
                        + " using priority, region, geo-distance, and SLA rules."
        );

        return new AutoDispatchResponse(
                "Automated dispatch completed successfully",
                incident.getId(),
                best.getId(),
                best.getResourceName(),
                incident.getPriority().name(),
                incident.getRegion() == null ? "UNKNOWN" : incident.getRegion().name(),
                round(distance),
                round(estimatedMinutes),
                slaText(incident.getPriority(), slaLimit),
                slaMet,
                escalationStatus
        );
    }

    private int regionPenalty(Incident incident, EmergencyResource resource) {

        if (incident.getRegion() == null || resource.getRegion() == null) {
            return 50;
        }

        if (incident.getRegion() == resource.getRegion()) {
            return 0;
        }

        return 100;
    }
    private String slaText(IncidentPriority priority, double minutes) {
        return switch (priority) {
            case CRITICAL -> "SLA-1: Immediate dispatch target within " + minutes + " minutes";
            case HIGH -> "SLA-2: High-priority dispatch target within " + minutes + " minutes";
            case MEDIUM -> "SLA-3: Standard dispatch target within " + minutes + " minutes";
            case LOW -> "SLA-4: Non-critical dispatch target within " + minutes + " minutes";
        };
    }

    private double distanceKm(double lat1, double lon1, double lat2, double lon2) {

        final int earthRadiusKm = 6371;

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a =
                Math.sin(dLat / 2) * Math.sin(dLat / 2)
                        + Math.cos(Math.toRadians(lat1))
                        * Math.cos(Math.toRadians(lat2))
                        * Math.sin(dLon / 2)
                        * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return earthRadiusKm * c;
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}