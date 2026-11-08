package com.emergencyconnectuae.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LocationUpdateRequest {

    private double latitude;
    private double longitude;
    private String location;
}