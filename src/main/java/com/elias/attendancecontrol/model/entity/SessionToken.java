package com.elias.attendancecontrol.model.entity;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;
@Entity
@Table(name = "session_tokens")
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class SessionToken extends Token {
    @NotNull(message = "El usuario es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @Size(max = 50, message = "La dirección IP no puede exceder 50 caracteres")
    @Column(name = "ip_address", length = 50)
    private String ipAddress;
    public SessionToken(Long id, String token, LocalDateTime expirationTime, Boolean active, LocalDateTime createdDate, String ipAddress, User user) {
        super(id, token, expirationTime, active, createdDate);
        this.ipAddress = ipAddress;
        this.user = user;
    }
}
