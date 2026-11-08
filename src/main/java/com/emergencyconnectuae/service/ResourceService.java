package com.emergencyconnectuae.service;

import com.emergencyconnectuae.dto.*;
import com.emergencyconnectuae.model.*;
import com.emergencyconnectuae.repository.ResourceRepository;
import com.emergencyconnectuae.repository.UserRepository;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ResourceService {

    private static final String AVAILABLE_RESOURCES_CACHE_KEY = "available_resources";

    private final ResourceRepository resourceRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;
    private final RedisCacheService redisCacheService;
    private final DashboardService dashboardService;

    public ResourceService(ResourceRepository resourceRepository,
                           UserRepository userRepository,
                           AuditLogService auditLogService,
                           RedisCacheService redisCacheService,
                           DashboardService dashboardService) {
        this.resourceRepository = resourceRepository;
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
        this.redisCacheService = redisCacheService;
        this.dashboardService = dashboardService;
    }

    public EmergencyResource createResource(ResourceRequest request, String performedBy) {

        User manager = userRepository.findByEmail(performedBy)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        EmergencyResource resource = EmergencyResource.builder()
                .resourceName(request.getResourceName())
                .type(request.getType())
                .location(request.getLocation())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .capacity(request.getCapacity())
                .region(resolveRegion(request.getRegion(), request.getLocation()))
                .status(ResourceStatus.AVAILABLE)
                .managedBy(manager)
                .build();

        EmergencyResource saved = resourceRepository.save(resource);

        clearResourceCaches();

        auditLogService.log(
                "CREATE_RESOURCE",
                performedBy,
                "EmergencyResource",
                saved.getId(),
                "Resource created by " + manager.getEmail() + ": " + saved.getResourceName()
        );

        dashboardService.clearDashboardCache();

        return saved;
    }

    public PaginatedResponse<EmergencyResource> getAllResources(int page, int size) {

        Page<EmergencyResource> resources = resourceRepository.findAll(PageRequest.of(page, size));

        return new PaginatedResponse<>(
                resources.getContent(),
                resources.getNumber(),
                resources.getSize(),
                resources.getTotalElements()
        );
    }

    public PaginatedResponse<EmergencyResource> getAvailableResources(int page, int size) {

        String cacheKey = AVAILABLE_RESOURCES_CACHE_KEY + ":page:" + page + ":size:" + size;

        Object cached = redisCacheService.get(cacheKey);

        if (cached != null) {
            return (PaginatedResponse<EmergencyResource>) cached;
        }

        Page<EmergencyResource> resources =
                resourceRepository.findByStatus(ResourceStatus.AVAILABLE, PageRequest.of(page, size));

        PaginatedResponse<EmergencyResource> response = new PaginatedResponse<>(
                resources.getContent(),
                resources.getNumber(),
                resources.getSize(),
                resources.getTotalElements()
        );

        redisCacheService.save(cacheKey, response, 5);

        return response;
    }

    public EmergencyResource getResourceById(Long id) {
        return resourceRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found"));
    }

    public EmergencyResource updateAvailability(Long id, ResourceStatus status, String performedBy) {

        if (status == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Resource status is required");
        }

        EmergencyResource resource = getResourceById(id);

        User currentUser = userRepository.findByEmail(performedBy)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        enforceResourceUpdatePermission(resource, currentUser);

        ResourceStatus oldStatus = resource.getStatus();
        resource.setStatus(status);

        EmergencyResource saved = resourceRepository.save(resource);

        clearResourceCaches();

        auditLogService.log(
                "UPDATE_RESOURCE_STATUS",
                performedBy,
                "EmergencyResource",
                saved.getId(),
                "Resource status changed from " + oldStatus + " to " + status
        );

        dashboardService.clearDashboardCache();

        return saved;
    }

    public EmergencyResource updateLiveLocation(Long id, LocationUpdateRequest request, String performedBy) {

        EmergencyResource resource = getResourceById(id);

        resource.setLatitude(request.getLatitude());
        resource.setLongitude(request.getLongitude());

        if (request.getLocation() != null && !request.getLocation().isBlank()) {
            resource.setLocation(request.getLocation());
        }

        EmergencyResource saved = resourceRepository.save(resource);

        clearResourceCaches();
        dashboardService.clearDashboardCache();

        auditLogService.log(
                "UPDATE_RESOURCE_LIVE_LOCATION",
                performedBy,
                "EmergencyResource",
                saved.getId(),
                "Live GPS location updated to lat=" + saved.getLatitude()
                        + ", lon=" + saved.getLongitude()
                        + ", area=" + saved.getLocation()
        );

        return saved;
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
    private void enforceResourceUpdatePermission(EmergencyResource resource, User currentUser) {

        if (currentUser.getRole() == Role.ADMIN) {
            return;
        }

        boolean isManager = resource.getManagedBy() != null
                && resource.getManagedBy().getEmail().equalsIgnoreCase(currentUser.getEmail());

        if (isManager) {
            return;
        }

        throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "You are not allowed to update this resource. Only the resource manager or admin can update it."
        );
    }
    
  
    private void clearResourceCaches() {
        redisCacheService.delete(AVAILABLE_RESOURCES_CACHE_KEY);
        redisCacheService.delete(AVAILABLE_RESOURCES_CACHE_KEY + ":page:0:size:10");
    }
}