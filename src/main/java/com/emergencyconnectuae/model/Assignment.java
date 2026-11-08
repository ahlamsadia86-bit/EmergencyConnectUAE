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
public class Assignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Incident incident;

    @ManyToOne
    private EmergencyResource resource;

    @ManyToOne
    private User assignedBy;

    private LocalDateTime assignedAt;
}
