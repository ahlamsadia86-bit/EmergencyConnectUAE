package com.emergencyconnectuae.repository;

import com.emergencyconnectuae.model.Incident;
import com.emergencyconnectuae.model.IncidentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IncidentRepository extends JpaRepository<Incident, Long> {

    Page<Incident> findByStatus(IncidentStatus status, Pageable pageable);
}
