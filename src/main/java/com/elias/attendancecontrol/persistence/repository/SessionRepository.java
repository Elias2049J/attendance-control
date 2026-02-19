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
    List<Session> findByActivity(Activity activity);
    List<Session> findBySessionDateBetween(LocalDate start, LocalDate end);
    List<Session> findByActivityAndSessionDateBetween(Activity activity, LocalDate start, LocalDate end);
    Optional<Session> findByActivityAndSessionDate(Activity activity, LocalDate date);
    boolean existsByActivityAndSessionDate(Activity activity, LocalDate date);
    List<Session> findByStatus(SessionStatus status);
    List<Session> findByActivityAndStatus(Activity activity, SessionStatus status);
    long countByActivityAndStatus(Activity activity, SessionStatus status);

    @Query("SELECT s FROM Session s WHERE s.activity.organization.id = :orgId")
    List<Session> findByActivityOrganizationId(@Param("orgId") Long organizationId);

    @Query("SELECT s FROM Session s WHERE s.sessionDate BETWEEN :startDate AND :endDate " +
           "AND s.activity.organization.id = :orgId")
    List<Session> findBySessionDateBetweenAndActivityOrganizationId(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("orgId") Long organizationId);
}
