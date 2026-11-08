package com.emergencyconnectuae.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AssignmentRequest {

    private Long incidentId;

    private Long resourceId;

    private Long assignedByUserId;
}