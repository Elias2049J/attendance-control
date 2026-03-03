package com.elias.attendancecontrol.persistence.repository;
import com.elias.attendancecontrol.model.entity.QRToken;
import com.elias.attendancecontrol.model.entity.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;
@Repository
public interface QrTokenRepository extends JpaRepository<QRToken, Long> {
    Optional<QRToken> findByToken(String token);
    Optional<QRToken> findByTokenAndActiveTrue(String token);
    Optional<QRToken> findBySession(Session session);
    Optional<QRToken> findBySessionAndActiveTrue(Session session);

    @Query("SELECT q FROM QRToken q JOIN FETCH q.session s JOIN FETCH s.activity a JOIN FETCH a.organization WHERE q.token = :token")
    Optional<QRToken> findByTokenWithSessionAndOrganization(@Param("token") String token);
}
