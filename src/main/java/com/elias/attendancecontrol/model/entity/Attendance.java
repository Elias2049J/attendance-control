package com.elias.attendancecontrol.model.entity;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;
@Entity
@Table(name = "attendances",
    uniqueConstraints = @UniqueConstraint(
        columnNames = {"session_id", "user_id"},
        name = "uk_attendance_session_user"
    ))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Attendance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotNull(message = "La sesión asociada es obligatoria")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private Session session;
    @NotNull(message = "El usuario es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @NotNull(message = "La hora de registro es obligatoria")
    @PastOrPresent(message = "La hora de registro no puede ser en el futuro")
    @Column(name = "registration_time", nullable = false)
    private LocalDateTime registrationTime;
    @NotNull(message = "El estado de asistencia es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AttendanceStatus status;
    @NotBlank(message = "El tipo de registro es obligatorio")
    @Size(max = 50, message = "El tipo de registro no puede exceder 50 caracteres")
    @Column(name = "registration_type", nullable = false, length = 50)
    private String registrationType;
    @Column(name = "created_date", nullable = false, updatable = false)
    private LocalDateTime createdDate;
    @PrePersist
    protected void onCreate() {
        createdDate = LocalDateTime.now();
        if (registrationTime == null) {
            registrationTime = LocalDateTime.now();
        }
    }
}
