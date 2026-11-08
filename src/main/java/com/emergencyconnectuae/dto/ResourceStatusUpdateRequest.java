package com.emergencyconnectuae.dto;

import com.emergencyconnectuae.model.ResourceStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResourceStatusUpdateRequest {

    private ResourceStatus status;
}