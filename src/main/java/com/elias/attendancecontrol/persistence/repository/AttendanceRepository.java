package com.elias.attendancecontrol.persistence.repository;
import com.elias.attendancecontrol.model.entity.Attendance;
import com.elias.attendancecontrol.model.entity.Session;
import com.elias.attendancecontrol.model.entity.User;
import com.elias.attendancecontrol.model.dto.ActivityAttendanceSummaryDTO;
import com.elias.attendancecontrol.model.dto.UserActivityReportDTO;
import com.elias.attendancecontrol.model.dto.ActivityUserReportDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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
    long countBySession(Session session);

    List<Attendance> findAllByUser_IdAndSession_Activity_Id(Long userId, Long sessionActivityId);

    // Reporte general de todas las actividades
    @Query("""
    SELECT new com.elias.attendancecontrol.model.dto.ActivityAttendanceSummaryDTO(
        a.id, a.name,
        COUNT(DISTINCT s.id),
        SUM(CASE WHEN att.status = 'PRESENT' THEN 1 ELSE 0 END),
        SUM(CASE WHEN att.status = 'ABSENT' THEN 1 ELSE 0 END),
        SUM(CASE WHEN att.status = 'LATE' THEN 1 ELSE 0 END),
        CAST(
               CASE WHEN COUNT(att.id) = 0
                    THEN 0
                    ELSE (SUM(CASE WHEN att.status = 'PRESENT' THEN 1 ELSE 0 END) * 1.0 / COUNT(att.id))
               END
           AS double)
    )
        FROM Activity a
        LEFT JOIN a.sessions s
        LEFT JOIN Attendance att ON att.session = s
        WHERE (s.sessionDate BETWEEN :startDate AND :endDate)
        GROUP BY a.id, a.name
    """)
    List<ActivityAttendanceSummaryDTO> getGeneralActivityReport(
            @Param("startDate") java.time.LocalDate startDate,
            @Param("endDate") java.time.LocalDate endDate
    );

    // Detalle de actividades inscritas para un usuario
    @Query("""
        SELECT new com.elias.attendancecontrol.model.dto.UserActivityReportDTO(
            u.id, u.name, a.id, a.name,
            COUNT(DISTINCT s.id),
            SUM(CASE WHEN att.status = 'PRESENT' THEN 1 ELSE 0 END),
            SUM(CASE WHEN att.status = 'ABSENT' THEN 1 ELSE 0 END),
            SUM(CASE WHEN att.status = 'LATE' THEN 1 ELSE 0 END)
        )
        FROM Enrollment e
        JOIN e.user u
        JOIN e.activity a
        LEFT JOIN a.sessions s
        LEFT JOIN Attendance att ON att.session = s AND att.user = u
        WHERE u.id = :userId AND (s.sessionDate BETWEEN :startDate AND :endDate)
        GROUP BY u.id, u.name, a.id, a.name
    """)
    List<UserActivityReportDTO> getUserActivityReport(@Param("userId") Long userId, @Param("startDate") java.time.LocalDate startDate, @Param("endDate") java.time.LocalDate endDate);

    // Detalle de todos los usuarios inscritos en una actividad
    @Query("""
        SELECT new com.elias.attendancecontrol.model.dto.ActivityUserReportDTO(
            a.id, a.name, u.id, u.name,
            COUNT(DISTINCT s.id),
            SUM(CASE WHEN att.status = 'PRESENT' THEN 1 ELSE 0 END),
            SUM(CASE WHEN att.status = 'ABSENT' THEN 1 ELSE 0 END),
            SUM(CASE WHEN att.status = 'LATE' THEN 1 ELSE 0 END)
        )
        FROM Enrollment e
        JOIN e.user u
        JOIN e.activity a
        LEFT JOIN a.sessions s
        LEFT JOIN Attendance att ON att.session = s AND att.user = u
        WHERE a.id = :activityId AND (s.sessionDate BETWEEN :startDate AND :endDate)
        GROUP BY a.id, a.name, u.id, u.name
    """)
    List<ActivityUserReportDTO> getActivityUserReport(@Param("activityId") Long activityId, @Param("startDate") java.time.LocalDate startDate, @Param("endDate") java.time.LocalDate endDate);
}
