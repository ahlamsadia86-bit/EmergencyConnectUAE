package com.emergencyconnectuae.service;

import com.emergencyconnectuae.dto.PaginatedResponse;
import com.emergencyconnectuae.model.AuditLog;
import com.emergencyconnectuae.repository.AuditLogRepository;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

@Service
public class AuditLogQueryService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogQueryService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public PaginatedResponse<AuditLog> getAuditLogs(int page, int size) {

        Page<AuditLog> logs = auditLogRepository.findAll(
                PageRequest.of(page, size, Sort.by("timestamp").descending())
        );

        return new PaginatedResponse<>(
                logs.getContent(),
                logs.getNumber(),
                logs.getSize(),
                logs.getTotalElements()
        );
    }
}