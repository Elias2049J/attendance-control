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
@Table(name = "organizations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Organization {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotBlank(message = "El nombre de la organización es obligatorio")
    @Size(min = 3, max = 200, message = "El nombre debe tener entre 3 y 200 caracteres")
    @Column(name = "name", nullable = false, length = 200)
    private String name;
    @NotBlank(message = "El identificador único es obligatorio")
    @Size(min = 3, max = 50, message = "El slug debe tener entre 3 y 50 caracteres")
    @Pattern(regexp = "^[a-z0-9-]+$", message = "El slug solo puede contener letras minúsculas, números y guiones")
    @Column(name = "slug", nullable = false, unique = true, length = 50)
    private String slug;
    @Size(max = 1000, message = "La descripción no puede exceder 1000 caracteres")
    @Column(name = "description", length = 1000)
    private String description;
    @Email(message = "El correo de contacto debe ser válido")
    @Size(max = 150, message = "El email no puede exceder 150 caracteres")
    @Column(name = "contact_email", length = 150)
    private String contactEmail;
    @Size(max = 20, message = "El teléfono no puede exceder 20 caracteres")
    @Column(name = "phone", length = 20)
    private String phone;
    @Size(max = 500, message = "La dirección no puede exceder 500 caracteres")
    @Column(name = "address", length = 500)
    private String address;
    @NotNull(message = "El estado activo es obligatorio")
    @Column(name = "active", nullable = false)
    private Boolean active = true;
    @NotNull(message = "El plan es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(name = "plan", nullable = false, length = 20)
    private OrganizationPlan plan = OrganizationPlan.FREE;
    @Min(value = 1, message = "El máximo de usuarios debe ser al menos 1")
    @Column(name = "max_users")
    private Integer maxUsers;
    @Min(value = 1, message = "El máximo de actividades debe ser al menos 1")
    @Column(name = "max_activities")
    private Integer maxActivities;
    @Column(name = "created_date", nullable = false, updatable = false)
    private LocalDateTime createdDate;
    @Column(name = "expiration_date")
    private LocalDateTime expirationDate;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private User owner;
    @OneToMany(mappedBy = "organization", cascade = CascadeType.ALL)
    private List<User> users = new ArrayList<>();
    @OneToMany(mappedBy = "organization", cascade = CascadeType.ALL)
    private List<Activity> activities = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdDate = LocalDateTime.now();
        if (active == null) {
            active = true;
        }
        if (plan == null) {
            plan = OrganizationPlan.FREE;
        }
    }

    public void applyPlanLimits(Integer freeMaxUsers, Integer freeMaxActivities,
                                Integer basicMaxUsers, Integer basicMaxActivities,
                                Integer premiumMaxUsers, Integer premiumMaxActivities) {
        switch (plan) {
            case FREE:
                this.maxUsers = freeMaxUsers;
                this.maxActivities = freeMaxActivities;
                break;
            case BASIC:
                this.maxUsers = basicMaxUsers;
                this.maxActivities = basicMaxActivities;
                break;
            case PREMIUM:
                this.maxUsers = premiumMaxUsers;
                this.maxActivities = premiumMaxActivities;
                break;
            case ENTERPRISE:
                this.maxUsers = Integer.MAX_VALUE;
                this.maxActivities = Integer.MAX_VALUE;
                break;
        }
    }
}
