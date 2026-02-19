package com.elias.attendancecontrol.persistence.repository;
import com.elias.attendancecontrol.model.entity.SessionToken;
import com.elias.attendancecontrol.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
@Repository
public interface SessionTokenRepository extends JpaRepository<SessionToken, Long> {
    Optional<SessionToken> findByTokenAndActiveTrue(String token);
    List<SessionToken> findByUser(User user);
    List<SessionToken> findByUserAndActiveTrue(User user);
    List<SessionToken> findByUser_UsernameAndActiveTrue(String username);
    void deleteByExpirationTimeBefore(LocalDateTime dateTime);
}
