package com.emergencyconnectuae.dto;

import com.emergencyconnectuae.model.IncidentPriority;
import com.emergencyconnectuae.model.ResourceType;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import com.emergencyconnectuae.model.Region;
@Getter
@Setter
public class IncidentRequest {

    @NotBlank
    private String title;

    @NotBlank
    private String description;

    @NotBlank
    private String location;

    private double latitude;

    private double longitude;

    private IncidentPriority priority;

    private Region region;
    private ResourceType requiredResourceType;
}