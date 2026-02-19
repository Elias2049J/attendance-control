package com.elias.attendancecontrol.model.entity;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;
import java.time.LocalDateTime;
@Entity
@Table(name = "activity_incidents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ActivityIncident {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotNull(message = "La actividad asociada es obligatoria")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_id", nullable = false)
    private Activity activity;
    @NotNull(message = "La fecha original es obligatoria")
    @Column(name = "original_date", nullable = false)
    private LocalDate originalDate;
    @Column(name = "new_date")
    private LocalDate newDate;
    @NotNull(message = "El indicador de cancelación es obligatorio")
    @Column(name = "cancelled", nullable = false)
    private Boolean cancelled = false;
    @Size(max = 500, message = "La razón no puede exceder 500 caracteres")
    @Column(name = "reason", length = 500)
    private String reason;
    @NotNull(message = "El usuario creador es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdByUser;
    @Column(name = "created_date", nullable = false, updatable = false)
    private LocalDateTime createdDate;
    @PrePersist
    protected void onCreate() {
        createdDate = LocalDateTime.now();
    }
}
