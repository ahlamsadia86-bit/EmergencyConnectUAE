package com.emergencyconnectuae.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AutoDispatchResponse {

    private String message;

    private Long incidentId;
    private Long assignedResourceId;
    private String assignedResourceName;

    private String priority;
    private String region;
    private double distanceKm;
    private double estimatedResponseMinutes;

    private String slaLevel;
    private boolean slaMet;
    private String escalationStatus;
}
