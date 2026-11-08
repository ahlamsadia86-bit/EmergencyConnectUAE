package com.emergencyconnectuae.dto;

import com.emergencyconnectuae.model.Region;
import com.emergencyconnectuae.model.ResourceType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResourceRequest {

    private String resourceName;

    private ResourceType type;

    private String location;

    private double latitude;

    private double longitude;

    private int capacity;
    
    private Region region;
}