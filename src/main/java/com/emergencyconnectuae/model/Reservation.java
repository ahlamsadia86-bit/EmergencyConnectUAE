
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
public class Reservation {
	
	@ManyToOne
    private User managedBy;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private EmergencyResource resource;

    @ManyToOne
    private Incident incident;

    private int reservedQuantity;

    private LocalDateTime reservedAt;
}