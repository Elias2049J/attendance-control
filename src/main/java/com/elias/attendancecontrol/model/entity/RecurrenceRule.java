package com.elias.attendancecontrol.model.entity;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;
import java.time.LocalTime;
@Entity
@Table(name = "recurrence_rules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RecurrenceRule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotNull(message = "La actividad asociada es obligatoria")
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_id", nullable = false, unique = true)
    private Activity activity;
    @NotNull(message = "El tipo de recurrencia es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(name = "recurrence_type", nullable = false, length = 20)
    private RecurrenceType recurrenceType;
    @Size(max = 100, message = "Los días de la semana no pueden exceder 100 caracteres")
    @Column(name = "days_of_week", length = 100)
    private String daysOfWeek;
    @NotNull(message = "La fecha de inicio es obligatoria")
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;
    @Column(name = "end_date")
    private LocalDate endDate;
    @NotNull(message = "La hora de inicio es obligatoria")
    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;
    @NotNull(message = "La hora de fin es obligatoria")
    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;
    @NotNull(message = "La tolerancia es obligatoria")
    @Min(value = 0, message = "La tolerancia no puede ser negativa")
    @Column(name = "tolerance_minutes", nullable = false)
    private Integer toleranceMinutes;
    public long getDurationMinutes() {
        if (startTime == null || endTime == null) {
            return 0;
        }
        return java.time.Duration.between(startTime, endTime).toMinutes();
    }
}
