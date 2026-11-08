package com.emergencyconnectuae.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmergencyResource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String resourceName;

    @Enumerated(EnumType.STRING)
    private ResourceType type;

    @Enumerated(EnumType.STRING)
    private ResourceStatus status;

    private String location;

    private int capacity;
    
    @Enumerated(EnumType.STRING)
    private Region region;
    
    private double latitude;
    private double longitude;
    
    

    @ManyToOne
    private User managedBy;
}