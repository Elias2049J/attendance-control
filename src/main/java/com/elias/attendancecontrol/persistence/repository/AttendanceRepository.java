package com.elias.attendancecontrol.persistence.repository;
import com.elias.attendancecontrol.model.entity.Attendance;
import com.elias.attendancecontrol.model.entity.Session;
import com.elias.attendancecontrol.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    List<Attendance> findBySession(Session session);
    List<Attendance> findByUser(User user);
    Optional<Attendance> findBySessionAndUser(Session session, User user);
    Optional<Attendance> findBySession_IdAndUser_Id(Long sessionId, Long userId);
    boolean existsBySessionAndUser(Session session, User user);
    boolean existsBySession_IdAndUser_Id(Long sessionId, Long userId);
    boolean existsBySession(Session session);
}
