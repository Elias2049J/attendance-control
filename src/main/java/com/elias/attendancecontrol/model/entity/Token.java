package com.elias.attendancecontrol.model.entity;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDateTime;
@Entity
@Table(name = "tokens")
@Inheritance(strategy = InheritanceType.JOINED)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public abstract class Token {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotBlank(message = "El token es obligatorio")
    @Size(max = 500, message = "El token no puede exceder 500 caracteres")
    @Column(name = "token", nullable = false, unique = true, length = 500)
    private String token;
    @NotNull(message = "La fecha de expiración es obligatoria")
    @FutureOrPresent(message = "La fecha de expiración debe ser en el futuro o presente")
    @Column(name = "expiration_time", nullable = false)
    private LocalDateTime expirationTime;
    @NotNull(message = "El estado activo es obligatorio")
    @Column(name = "active", nullable = false)
    private Boolean active = true;
    @Column(name = "created_date", nullable = false, updatable = false)
    private LocalDateTime createdDate;
    @PrePersist
    protected void onCreate() {
        createdDate = LocalDateTime.now();
    }
}
