package com.emergencyconnectuae.service;

import com.emergencyconnectuae.model.IncidentStatus;
import com.emergencyconnectuae.model.ResourceStatus;
import com.emergencyconnectuae.repository.IncidentRepository;
import com.emergencyconnectuae.repository.ResourceRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class DashboardService {

    private static final String DASHBOARD_CACHE_KEY = "dashboard_summary";

    private final IncidentRepository incidentRepository;
    private final ResourceRepository resourceRepository;
    private final RedisCacheService redisCacheService;

    public DashboardService(IncidentRepository incidentRepository,
                            ResourceRepository resourceRepository,
                            RedisCacheService redisCacheService) {
        this.incidentRepository = incidentRepository;
        this.resourceRepository = resourceRepository;
        this.redisCacheService = redisCacheService;
    }

    public Map<String, Object> getDashboardSummary() {

        try {
            Object cached = redisCacheService.get(DASHBOARD_CACHE_KEY);

            if (cached != null) {
                return (Map<String, Object>) cached;
            }

            Map<String, Object> summary = new HashMap<>();

            summary.put("openIncidents",
                    incidentRepository.findByStatus(IncidentStatus.OPEN, PageRequest.of(0, 1000)).getTotalElements());

            summary.put("inProgressIncidents",
                    incidentRepository.findByStatus(IncidentStatus.IN_PROGRESS, PageRequest.of(0, 1000)).getTotalElements());

            summary.put("resolvedIncidents",
                    incidentRepository.findByStatus(IncidentStatus.RESOLVED, PageRequest.of(0, 1000)).getTotalElements());

            summary.put("availableResources",
                    resourceRepository.findByStatus(ResourceStatus.AVAILABLE, PageRequest.of(0, 1000)).getTotalElements());

            summary.put("assignedResources",
                    resourceRepository.findByStatus(ResourceStatus.ASSIGNED, PageRequest.of(0, 1000)).getTotalElements());

            summary.put("mode", "NORMAL_REDIS_CACHED");

            redisCacheService.save(DASHBOARD_CACHE_KEY, summary, 3);

            return summary;

        } catch (Exception e) {

            Map<String, Object> fallback = new HashMap<>();

            fallback.put("openIncidents",
                    incidentRepository.findByStatus(IncidentStatus.OPEN, PageRequest.of(0, 1000)).getTotalElements());

            fallback.put("inProgressIncidents",
                    incidentRepository.findByStatus(IncidentStatus.IN_PROGRESS, PageRequest.of(0, 1000)).getTotalElements());

            fallback.put("resolvedIncidents",
                    incidentRepository.findByStatus(IncidentStatus.RESOLVED, PageRequest.of(0, 1000)).getTotalElements());

            fallback.put("availableResources",
                    resourceRepository.findByStatus(ResourceStatus.AVAILABLE, PageRequest.of(0, 1000)).getTotalElements());

            fallback.put("assignedResources",
                    resourceRepository.findByStatus(ResourceStatus.ASSIGNED, PageRequest.of(0, 1000)).getTotalElements());

            fallback.put("mode", "GRACEFUL_DEGRADATION_DB_FALLBACK");

            return fallback;
        }
    }

    public void clearDashboardCache() {
        redisCacheService.delete(DASHBOARD_CACHE_KEY);
    }
    
    public Object getActiveIncidents() {

        String cacheKey = "active_incidents";

        Object cached = redisCacheService.get(cacheKey);

        if (cached != null) {
            return cached;
        }

        var activeIncidents = incidentRepository.findByStatus(
                IncidentStatus.OPEN,
                PageRequest.of(0, 100)
        ).getContent();

        redisCacheService.save(cacheKey, activeIncidents, 3);

        return activeIncidents;
    }

    public void clearActiveIncidentsCache() {
        redisCacheService.delete("active_incidents");
    }
}