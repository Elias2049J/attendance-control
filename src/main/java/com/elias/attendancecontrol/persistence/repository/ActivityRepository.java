package com.elias.attendancecontrol.persistence.repository;
import com.elias.attendancecontrol.model.entity.Activity;
import com.elias.attendancecontrol.model.entity.ActivityStatus;
import com.elias.attendancecontrol.model.entity.Organization;
import com.elias.attendancecontrol.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
@Repository
public interface ActivityRepository extends JpaRepository<Activity, Long> {
    List<Activity> findByStatus(ActivityStatus status);
    List<Activity> findByStatusIn(List<ActivityStatus> statuses);
    List<Activity> findByResponsible(User user);
    List<Activity> findByStatusAndResponsible(ActivityStatus status, User user);
    List<Activity> findByOrganization(Organization organization);
    List<Activity> findByOrganizationAndStatus(Organization organization, ActivityStatus status);
    List<Activity> findByOrganizationAndResponsible(Organization organization, User responsible);
    @Query("SELECT a FROM Activity a WHERE a.organization.id = :orgId")
    List<Activity> findByOrganizationId(@Param("orgId") Long organizationId);
    @Query("SELECT a FROM Activity a WHERE a.organization.id = :orgId AND a.status = :status")
    List<Activity> findByOrganizationIdAndStatus(@Param("orgId") Long organizationId, @Param("status") ActivityStatus status);
    @Query("SELECT a FROM Activity a WHERE a.organization.id = :orgId AND a.status IN :statuses")
    List<Activity> findByOrganizationIdAndStatusIn(@Param("orgId") Long organizationId, @Param("statuses") List<ActivityStatus> statuses);
    List<Activity> findByResponsibleId(Long userId);
    @Query("SELECT DISTINCT a FROM Activity a JOIN a.sessions s WHERE " +
           "s.sessionDate BETWEEN :startDate AND :endDate")
    List<Activity> findBySessionsDateBetween(@Param("startDate") LocalDate startDate,
                                             @Param("endDate") LocalDate endDate);

    @Query("SELECT DISTINCT a FROM Activity a JOIN a.sessions s " +
           "WHERE a.organization.id = :orgId " +
           "AND s.sessionDate BETWEEN :startDate AND :endDate")
    List<Activity> findByOrganizationIdAndSessionsDateBetween(
        @Param("orgId") Long organizationId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate);
}
