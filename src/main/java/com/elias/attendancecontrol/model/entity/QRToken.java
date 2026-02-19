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
@Table(name = "qr_tokens")
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class QRToken extends Token {
    @NotNull(message = "La sesión asociada es obligatoria")
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false, unique = true)
    private Session session;
    @NotNull(message = "La fecha de inicio de validez es obligatoria")
    @Column(name = "valid_from", nullable = false)
    private LocalDateTime validFrom;
    @NotNull(message = "La fecha de fin de validez es obligatoria")
    @Column(name = "valid_until", nullable = false)
    private LocalDateTime validUntil;
    public QRToken(Long id, String token, LocalDateTime expirationTime, Boolean active, LocalDateTime createdDate, Session session, LocalDateTime validFrom, LocalDateTime validUntil) {
        super(id, token, expirationTime, active, createdDate);
        this.session = session;
        this.validFrom = validFrom;
        this.validUntil = validUntil;
    }
}
