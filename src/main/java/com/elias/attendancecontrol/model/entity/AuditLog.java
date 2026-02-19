package com.elias.attendancecontrol.model.entity;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDateTime;
@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    private Session session;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    private Organization organization;
    @NotBlank(message = "El tipo de evento es obligatorio")
    @Size(max = 100, message = "El tipo de evento no puede exceder 100 caracteres")
    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;
    @NotNull(message = "La fecha del evento es obligatoria")
    @PastOrPresent(message = "La fecha del evento no puede ser en el futuro")
    @Column(name = "event_date", nullable = false)
    private LocalDateTime eventDate;
    @NotBlank(message = "La descripción es obligatoria")
    @Size(max = 500, message = "La descripción no puede exceder 500 caracteres")
    @Column(name = "description", nullable = false, length = 500)
    private String description;
    @Size(max = 50, message = "La dirección IP no puede exceder 50 caracteres")
    @Column(name = "ip_address", length = 50)
    private String ipAddress;
    @Size(max = 10000, message = "Los detalles no pueden exceder 10000 caracteres")
    @Column(name = "details", columnDefinition = "TEXT")
    private String details;
    @PrePersist
    protected void onCreate() {
        if (eventDate == null) {
            eventDate = LocalDateTime.now();
        }
    }
}
