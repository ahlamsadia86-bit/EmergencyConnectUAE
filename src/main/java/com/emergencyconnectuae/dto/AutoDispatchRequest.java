package com.emergencyconnectuae.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AutoDispatchRequest {
    private Long incidentId;
    private Long assignedByUserId;
}
