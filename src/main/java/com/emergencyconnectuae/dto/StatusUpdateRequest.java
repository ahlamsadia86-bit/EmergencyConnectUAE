package com.emergencyconnectuae.dto;

import com.emergencyconnectuae.model.IncidentStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StatusUpdateRequest {

    private IncidentStatus status;
}