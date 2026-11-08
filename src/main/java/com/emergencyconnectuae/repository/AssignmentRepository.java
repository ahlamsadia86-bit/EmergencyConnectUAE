package com.emergencyconnectuae.repository;

import com.emergencyconnectuae.model.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssignmentRepository extends JpaRepository<Assignment, Long> {
}