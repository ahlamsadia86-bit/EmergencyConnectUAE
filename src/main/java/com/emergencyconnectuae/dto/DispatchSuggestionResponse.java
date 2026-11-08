package com.emergencyconnectuae.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DispatchSuggestionResponse {

    private Long incidentId;
    private String incidentTitle;
    private String incidentPriority;
    private String requiredResourceType;
    private String incidentLocation;

    private Long suggestedResourceId;
    private String suggestedResourceName;
    private String resourceType;
    private String resourceLocation;

    private double distanceKm;
    private double estimatedResponseMinutes;

    private String slaLevel;
    private boolean slaMet;
    private String escalationStatus;
    private String recommendationReason;
}