package com.elias.attendancecontrol.persistence.repository;
import com.elias.attendancecontrol.model.entity.Activity;
import com.elias.attendancecontrol.model.entity.Session;
import com.elias.attendancecontrol.model.entity.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
@Repository
public interface SessionRepository extends JpaRepository<Session, Long> {
    @Query("SELECT s FROM Session s JOIN FETCH s.activity a JOIN FETCH a.organization LEFT JOIN FETCH a.responsible WHERE s.activity = :activity ORDER BY s.sessionDate ASC")
    List<Session> findByActivityOrderBySessionDateAsc(@Param("activity") Activity activity);

    @Query("SELECT s FROM Session s JOIN FETCH s.activity a JOIN FETCH a.organization LEFT JOIN FETCH a.responsible WHERE s.sessionDate BETWEEN :start AND :end ORDER BY s.sessionDate ASC")
    List<Session> findBySessionDateBetweenOrderBySessionDateAsc(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("SELECT s FROM Session s JOIN FETCH s.activity a JOIN FETCH a.organization LEFT JOIN FETCH a.responsible WHERE s.activity = :activity AND s.sessionDate BETWEEN :start AND :end")
    List<Session> findByActivityAndSessionDateBetween(@Param("activity") Activity activity, @Param("start") LocalDate start, @Param("end") LocalDate end);

    Optional<Session> findByActivityAndSessionDate(Activity activity, LocalDate date);
    boolean existsByActivityAndSessionDate(Activity activity, LocalDate date);

    @Query("SELECT s FROM Session s JOIN FETCH s.activity a JOIN FETCH a.organization LEFT JOIN FETCH a.responsible WHERE s.status = :status")
    List<Session> findByStatus(@Param("status") SessionStatus status);

    @Query("SELECT s FROM Session s JOIN FETCH s.activity a JOIN FETCH a.organization LEFT JOIN FETCH a.responsible WHERE s.activity = :activity AND s.status = :status")
    List<Session> findByActivityAndStatus(@Param("activity") Activity activity, @Param("status") SessionStatus status);

    long countByActivityAndStatus(Activity activity, SessionStatus status);

    @Query("SELECT s FROM Session s JOIN FETCH s.activity a JOIN FETCH a.organization LEFT JOIN FETCH a.responsible WHERE a.organization.id = :orgId ORDER BY s.sessionDate ASC")
    List<Session> findByActivityOrganizationId(@Param("orgId") Long organizationId);

    @Query("SELECT s FROM Session s JOIN FETCH s.activity a JOIN FETCH a.organization LEFT JOIN FETCH a.responsible WHERE s.sessionDate BETWEEN :startDate AND :endDate AND a.organization.id = :orgId ORDER BY s.sessionDate ASC")
    List<Session> findBySessionDateBetweenAndActivityOrganizationId(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("orgId") Long organizationId);

    @Query("SELECT s FROM Session s JOIN FETCH s.activity a JOIN FETCH a.organization LEFT JOIN FETCH a.responsible WHERE s.id = :id")
    Optional<Session> findByIdWithActivityAndOrganization(@Param("id") Long id);

    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END " +
           "FROM Session s WHERE s.activity.id = :activityId " +
           "AND s.status NOT IN ('CLOSED', 'CANCELLED')")
    boolean existsActiveOrPlannedSessionsByActivityId(@Param("activityId") Long activityId);
}
