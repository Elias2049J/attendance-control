package com.elias.attendancecontrol.model.entity;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;
@Entity
@Table(name = "enrollments",
    uniqueConstraints = @UniqueConstraint(
        columnNames = {"activity_id", "user_id"},
        name = "uk_enrollment_activity_user"
    ))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Enrollment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotNull(message = "La actividad es obligatoria")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_id", nullable = false)
    private Activity activity;
    @NotNull(message = "El usuario es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @NotNull(message = "La fecha de inscripción es obligatoria")
    @PastOrPresent(message = "La fecha de inscripción no puede ser en el futuro")
    @Column(name = "enrollment_date", nullable = false)
    private LocalDateTime enrollmentDate;
    @NotNull(message = "El estado de inscripción es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private EnrollmentStatus status = EnrollmentStatus.ENROLLED;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enrolled_by_user_id")
    private User enrolledByUser;
    @Column(name = "created_date", nullable = false, updatable = false)
    private LocalDateTime createdDate;
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
    @PrePersist
    protected void onCreate() {
        createdDate = LocalDateTime.now();
        if (enrollmentDate == null) {
            enrollmentDate = LocalDateTime.now();
        }
    }
}
