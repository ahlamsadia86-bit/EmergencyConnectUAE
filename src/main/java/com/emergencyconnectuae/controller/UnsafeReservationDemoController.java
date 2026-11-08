package com.emergencyconnectuae.controller;

import com.emergencyconnectuae.dto.ReservationRequest;
import com.emergencyconnectuae.model.*;
import com.emergencyconnectuae.repository.*;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/unsafe-demo")
public class UnsafeReservationDemoController {

    private final ReservationRepository reservationRepository;
    private final IncidentRepository incidentRepository;
    private final ResourceRepository resourceRepository;
    private final UserRepository userRepository;

    public UnsafeReservationDemoController(ReservationRepository reservationRepository,
                                           IncidentRepository incidentRepository,
                                           ResourceRepository resourceRepository,
                                           UserRepository userRepository) {
        this.reservationRepository = reservationRepository;
        this.incidentRepository = incidentRepository;
        this.resourceRepository = resourceRepository;
        this.userRepository = userRepository;
    }

    @PostMapping("/reserve-without-lock")
    @PreAuthorize("hasAnyRole('HOSPITAL_ADMIN','DISPATCHER','ADMIN')")
    public ResponseEntity<Reservation> reserveWithoutLock(@Valid @RequestBody ReservationRequest request) {

        Incident incident = incidentRepository.findById(request.getIncidentId())
                .orElseThrow(() -> new RuntimeException("Incident not found"));

        EmergencyResource resource = resourceRepository.findById(request.getResourceId())
                .orElseThrow(() -> new RuntimeException("Resource not found"));

        User user = userRepository.findById(1L)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (resource.getCapacity() < request.getReservedQuantity()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        resource.setCapacity(resource.getCapacity() - request.getReservedQuantity());

        if (resource.getCapacity() <= 0) {
            resource.setStatus(ResourceStatus.RESERVED);
        }

        resourceRepository.save(resource);

        Reservation reservation = Reservation.builder()
                .incident(incident)
                .resource(resource)
                .managedBy(user)
                .reservedQuantity(request.getReservedQuantity())
                .reservedAt(LocalDateTime.now())
                .build();

        Reservation saved = reservationRepository.save(reservation);

        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }
}