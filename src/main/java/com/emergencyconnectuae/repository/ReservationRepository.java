package com.emergencyconnectuae.repository;

import com.emergencyconnectuae.model.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
}