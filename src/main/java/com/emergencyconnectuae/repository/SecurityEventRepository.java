package com.emergencyconnectuae.repository;

import com.emergencyconnectuae.model.SecurityEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SecurityEventRepository extends JpaRepository<SecurityEvent, Long> {
    long countByEventType(String eventType);
}
