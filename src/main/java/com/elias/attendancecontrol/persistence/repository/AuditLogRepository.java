package com.elias.attendancecontrol.persistence.repository;
import com.elias.attendancecontrol.model.entity.AuditLog;
import com.elias.attendancecontrol.model.entity.Session;
import com.elias.attendancecontrol.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findByUser(User user);
    List<AuditLog> findBySession(Session session);
    List<AuditLog> findByEventType(String eventType);
    List<AuditLog> findByEventDateBetween(LocalDateTime start, LocalDateTime end);
    List<AuditLog> findByUserAndEventDateBetween(User user, LocalDateTime start, LocalDateTime end);
    @Query("SELECT a FROM AuditLog a WHERE a.user.organization.id = :orgId ORDER BY a.eventDate DESC")
    List<AuditLog> findByOrganization(@Param("orgId") Long organizationId);
    @Query("SELECT a FROM AuditLog a WHERE " +
           "(:userId IS NULL OR a.user.id = :userId) AND " +
           "(:eventType IS NULL OR a.eventType = :eventType) AND " +
           "(:startDate IS NULL OR a.eventDate >= :startDate) AND " +
           "(:endDate IS NULL OR a.eventDate <= :endDate) " +
           "ORDER BY a.eventDate DESC")
    List<AuditLog> searchLogs(@Param("userId") Long userId,
                              @Param("eventType") String eventType,
                              @Param("startDate") LocalDateTime startDate,
                              @Param("endDate") LocalDateTime endDate);
    @Query("SELECT a FROM AuditLog a WHERE " +
           "a.user.organization.id = :orgId AND " +
           "(:userId IS NULL OR a.user.id = :userId) AND " +
           "(:eventType IS NULL OR a.eventType = :eventType) AND " +
           "(:startDate IS NULL OR a.eventDate >= :startDate) AND " +
           "(:endDate IS NULL OR a.eventDate <= :endDate) " +
           "ORDER BY a.eventDate DESC")
    List<AuditLog> searchLogsByOrganization(@Param("orgId") Long organizationId,
                                            @Param("userId") Long userId,
                                            @Param("eventType") String eventType,
                                            @Param("startDate") LocalDateTime startDate,
                                            @Param("endDate") LocalDateTime endDate);
}
