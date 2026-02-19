package com.elias.attendancecontrol.model.entity;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
@Entity
@Table(name = "activities")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Activity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotBlank(message = "El nombre de la actividad es obligatorio")
    @Size(min = 3, max = 200, message = "El nombre de la actividad debe tener entre 3 y 200 caracteres")
    @Column(name = "name", nullable = false, length = 200)
    private String name;
    @Size(max = 5000, message = "La descripción no puede exceder 5000 caracteres")
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    @Column(name = "requires_enrollment", nullable = false)
    private Boolean requiresEnrollment = false;
    @Column(name = "created_date", nullable = false, updatable = false)
    private LocalDateTime createdDate;
    @NotNull(message = "El estado es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ActivityStatus status = ActivityStatus.DRAFT;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    private Organization organization;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsible_id")
    private User responsible;
    @OneToOne(mappedBy = "activity", cascade = CascadeType.ALL, orphanRemoval = true)
    private RecurrenceRule recurrenceRule;
    @OneToMany(mappedBy = "activity", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ActivityIncident> exceptions = new ArrayList<>();
    @OneToMany(mappedBy = "activity", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Session> sessions = new ArrayList<>();
    @OneToMany(mappedBy = "activity", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Enrollment> enrollments = new ArrayList<>();
    @PrePersist
    protected void onCreate() {
        createdDate = LocalDateTime.now();
    }
}
