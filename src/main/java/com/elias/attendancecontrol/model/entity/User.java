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
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "uuid", unique = true, updatable = false)
    private UUID uuid;
    @NotBlank(message = "El nombre de usuario es obligatorio")
    @Size(min = 3, max = 50, message = "El nombre de usuario debe tener entre 3 y 50 caracteres")
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "El nombre de usuario solo puede contener letras, números, puntos, guiones y guiones bajos")
    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;
    @Column(name = "password", nullable = false)
    private String password;
    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres")
    @Column(name = "name", nullable = false, length = 100)
    private String name;
    @NotBlank(message = "El apellido es obligatorio")
    @Size(min = 2, max = 100, message = "El apellido debe tener entre 2 y 100 caracteres")
    @Column(name = "lastname", nullable = false, length = 100)
    private String lastname;
    @NotBlank(message = "El correo electrónico es obligatorio")
    @Email(message = "El correo electrónico debe ser válido")
    @Size(max = 150, message = "El correo electrónico no puede exceder 150 caracteres")
    @Column(name = "email", nullable = false, unique = true, length = 150)
    private String email;
    @NotNull(message = "El estado activo es obligatorio")
    @Column(name = "active", nullable = false)
    private Boolean active = true;
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private SystemRole systemRole;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    private Organization organization;
    @Enumerated(EnumType.STRING)
    @Column(name = "organization_role", length = 20)
    private OrganizationRole organizationRole;
    @Column(name = "created_date", nullable = false, updatable = false)
    private LocalDateTime createdDate;
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SessionToken> sessionTokens = new ArrayList<>();
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<Attendance> attendances = new ArrayList<>();
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<Enrollment> enrollments = new ArrayList<>();
    @OneToMany(mappedBy = "responsible", cascade = CascadeType.ALL)
    private List<Activity> responsibleActivities = new ArrayList<>();
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<AuditLog> auditLogs = new ArrayList<>();
    @OneToMany(mappedBy = "createdByUser", cascade = CascadeType.ALL)
    private List<ActivityIncident> activityIncidents = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (uuid == null) uuid = UUID.randomUUID();
        createdDate = LocalDateTime.now();
    }

    public String getFullname() {
        return name + " " + lastname;
    }

    public boolean isPasswordValid() {
        return (password != null) && !password.isBlank()
                && !password.equalsIgnoreCase("password");
    }

    public boolean hasDefaultPassword() {
        return password != null && password.equalsIgnoreCase("password");
    }
}
