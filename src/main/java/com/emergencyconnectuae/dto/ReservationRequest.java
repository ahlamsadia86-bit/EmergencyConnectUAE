package com.emergencyconnectuae.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReservationRequest {

    private Long incidentId;

    private Long resourceId;

    private int reservedQuantity;
}