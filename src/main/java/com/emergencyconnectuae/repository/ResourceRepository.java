package com.emergencyconnectuae.repository;

import com.emergencyconnectuae.model.EmergencyResource;
import com.emergencyconnectuae.model.ResourceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResourceRepository extends JpaRepository<EmergencyResource, Long> {

    Page<EmergencyResource> findByStatus(ResourceStatus status, Pageable pageable);
}