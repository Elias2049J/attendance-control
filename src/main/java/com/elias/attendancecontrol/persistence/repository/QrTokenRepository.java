package com.elias.attendancecontrol.persistence.repository;
import com.elias.attendancecontrol.model.entity.QRToken;
import com.elias.attendancecontrol.model.entity.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
@Repository
public interface QrTokenRepository extends JpaRepository<QRToken, Long> {
    Optional<QRToken> findByToken(String token);
    Optional<QRToken> findByTokenAndActiveTrue(String token);
    Optional<QRToken> findBySession(Session session);
    Optional<QRToken> findBySessionAndActiveTrue(Session session);
}
