package com.elias.attendancecontrol.persistence.repository;
import com.elias.attendancecontrol.model.entity.Activity;
import com.elias.attendancecontrol.model.entity.Enrollment;
import com.elias.attendancecontrol.model.entity.EnrollmentStatus;
import com.elias.attendancecontrol.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
    List<Enrollment> findByActivity(Activity activity);
    List<Enrollment> findAllByUser(User user);
    List<Enrollment> findByActivityAndStatus(Activity activity, EnrollmentStatus status);
    Optional<Enrollment> findByActivityAndUser(Activity activity, User user);
    boolean existsByActivityAndUser(Activity activity, User user);
    boolean existsByActivityAndUserAndStatus(Activity activity, User user, EnrollmentStatus status);
    @Query("SELECT e.user FROM Enrollment e WHERE e.activity = :activity AND e.status = :status")
    List<User> findUsersByActivityAndStatus(@Param("activity") Activity activity,
                                            @Param("status") EnrollmentStatus status);
    @Query("SELECT e.activity FROM Enrollment e WHERE e.user = :user AND e.status = :status ORDER BY e.activity.recurrenceRule.startDate ASC, e.activity.id DESC")
    List<Activity> findActivitiesByUserAndStatus(@Param("user") User user,
                                                  @Param("status") EnrollmentStatus status);

    @Query("SELECT DISTINCT a FROM Activity a LEFT JOIN FETCH a.responsible LEFT JOIN FETCH a.recurrenceRule WHERE a.id IN (SELECT e.activity.id FROM Enrollment e WHERE e.user = :user AND e.status = :status) ORDER BY a.recurrenceRule.startDate ASC, a.id DESC")
    List<Activity> findActivitiesByUserAndStatusWithDetails(@Param("user") User user,
                                                             @Param("status") EnrollmentStatus status);
    long countByActivityAndStatus(Activity activity, EnrollmentStatus status);
}
