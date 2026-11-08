package com.emergencyconnectuae.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Incident {
	
	@ManyToOne
	private User reportedBy;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Column(length = 1000)
    private String description;

    private String location;

    @Enumerated(EnumType.STRING)
    private IncidentStatus status;

    @Enumerated(EnumType.STRING)
    private IncidentPriority priority;

    private LocalDateTime reportedAt;

    private LocalDateTime updatedAt;
    
    private double latitude;
    private double longitude;
    
    @Enumerated(EnumType.STRING)
    private ResourceType requiredResourceType;
    
    @Enumerated(EnumType.STRING)
    private Region region;
}
