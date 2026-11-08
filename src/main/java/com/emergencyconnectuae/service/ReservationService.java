package com.emergencyconnectuae.service;

import com.emergencyconnectuae.dto.ReservationRequest;
import com.emergencyconnectuae.model.*;
import com.emergencyconnectuae.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.UUID;
import com.emergencyconnectuae.dto.PaginatedResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
@Service
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final IncidentRepository incidentRepository;
    private final ResourceRepository resourceRepository;
    private final RedisLockService redisLockService;
    private final RedisCacheService redisCacheService;
    private final DashboardService dashboardService;
    private final AuditLogService auditLogService;
    private final UserRepository userRepository;

    public ReservationService(
            ReservationRepository reservationRepository,
            IncidentRepository incidentRepository,
            ResourceRepository resourceRepository,
            RedisLockService redisLockService,
            RedisCacheService redisCacheService,
            DashboardService dashboardService,
            AuditLogService auditLogService,
            UserRepository userRepository) {

        this.reservationRepository = reservationRepository;
        this.incidentRepository = incidentRepository;
        this.resourceRepository = resourceRepository;
        this.redisLockService = redisLockService;
        this.redisCacheService = redisCacheService;
        this.dashboardService = dashboardService;
        this.auditLogService = auditLogService;
        this.userRepository = userRepository;
    }
    public Reservation reserveResource(ReservationRequest request, String performedBy) {
    	
    	User user = userRepository.findByEmail(performedBy)
    	        .orElseThrow(() ->
    	                new ResponseStatusException(
    	                        HttpStatus.NOT_FOUND,
    	                        "User not found"
    	                ));

        if (request.getReservedQuantity() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reserved quantity must be greater than zero");
        }

        String lockKey = "lock:reservation-resource:" + request.getResourceId();
        String lockValue = UUID.randomUUID().toString();

        boolean locked = redisLockService.acquireLock(lockKey, lockValue, 10);

        if (!locked) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Resource is currently being reserved by another user"
            );
        }

        try {
            Incident incident = incidentRepository.findById(request.getIncidentId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Incident not found"));

            EmergencyResource resource = resourceRepository.findById(request.getResourceId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found"));

            if (incident.getStatus() == IncidentStatus.RESOLVED) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot reserve resource for resolved incident");
            }

            if (resource.getStatus() == ResourceStatus.OUT_OF_SERVICE) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Resource is out of service");
            }

            if (request.getReservedQuantity() > resource.getCapacity()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Not enough capacity available");
            }

            resource.setCapacity(resource.getCapacity() - request.getReservedQuantity());

            if (resource.getCapacity() == 0) {
                resource.setStatus(ResourceStatus.RESERVED);
            }

            resourceRepository.save(resource);

            Reservation reservation = Reservation.builder()
                    .incident(incident)
                    .resource(resource)
                    .reservedQuantity(request.getReservedQuantity())
                    .reservedAt(LocalDateTime.now())
                    .managedBy(user)
                    .build();
            Reservation saved = reservationRepository.save(reservation);

            redisCacheService.delete("available_resources");
            redisCacheService.delete("available_resources:page:0:size:10");
            dashboardService.clearDashboardCache();

            auditLogService.log(
                    "RESERVE_RESOURCE",
                    performedBy,
                    "Reservation",
                    saved.getId(),
                    "Reserved quantity " + request.getReservedQuantity()
                            + " from resource " + resource.getResourceName()
            );

            return saved;

        } finally {
            redisLockService.releaseLock(lockKey, lockValue);
        }
    }
    
    public PaginatedResponse<Reservation> getReservations(int page, int size) {

        Page<Reservation> reservations = reservationRepository.findAll(PageRequest.of(page, size));

        return new PaginatedResponse<>(
                reservations.getContent(),
                reservations.getNumber(),
                reservations.getSize(),
                reservations.getTotalElements()
        );
    }
}