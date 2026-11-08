package com.emergencyconnectuae.controller;

import com.emergencyconnectuae.dto.ReservationRequest;
import com.emergencyconnectuae.model.Reservation;
import com.emergencyconnectuae.service.ReservationService;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.emergencyconnectuae.dto.PaginatedResponse;
@RestController
@RequestMapping("/api/reservations")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('DISPATCHER','HOSPITAL_ADMIN','ADMIN')")
    public ResponseEntity<Reservation> reserveResource(@RequestBody ReservationRequest request,
                                                       Authentication authentication) {

        Reservation reservation = reservationService.reserveResource(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(reservation);
    }
    
    @GetMapping
    @PreAuthorize("hasAnyRole('DISPATCHER','HOSPITAL_ADMIN','ADMIN')")
    public ResponseEntity<PaginatedResponse<Reservation>> getReservations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return ResponseEntity.ok(reservationService.getReservations(page, size));
    }
}