package com.elias.attendancecontrol.model.entity;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
@Entity
@Table(name = "sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Session {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotNull(message = "La actividad asociada es obligatoria")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_id", nullable = false)
    private Activity activity;
    @NotNull(message = "La fecha de la sesión es obligatoria")
    @Column(name = "session_date", nullable = false)
    private LocalDate sessionDate;
    @NotNull(message = "La hora de inicio es obligatoria")
    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;
    @NotNull(message = "La hora de fin es obligatoria")
    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;
    @NotNull(message = "El estado de la sesión es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SessionStatus status = SessionStatus.PLANNED;
    @NotNull(message = "La tolerancia es obligatoria")
    @Min(value = 0, message = "La tolerancia no puede ser negativa")
    @Column(name = "tolerance_minutes", nullable = false)
    private Integer toleranceMinutes;
    @Column(name = "created_date", nullable = false, updatable = false)
    private LocalDateTime createdDate;
    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Attendance> attendances = new ArrayList<>();
    @OneToOne(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    private QRToken qrToken;
    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL)
    private List<AuditLog> auditLogs = new ArrayList<>();
    @PrePersist
    protected void onCreate() {
        createdDate = LocalDateTime.now();
    }
    public long getDurationMinutes() {
        if (startTime == null || endTime == null) {
            return 0;
        }
        return Duration.between(startTime, endTime).toMinutes();
    }
}
