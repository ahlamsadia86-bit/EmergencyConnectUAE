package com.emergencyconnectuae.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MfaRequest {

    @Email
    @NotBlank
    private String email;

    @NotBlank
    private String code;
}