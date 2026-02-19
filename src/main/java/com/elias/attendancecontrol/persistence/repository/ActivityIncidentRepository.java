package com.elias.attendancecontrol.persistence.repository;
import com.elias.attendancecontrol.model.entity.Activity;
import com.elias.attendancecontrol.model.entity.ActivityIncident;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
@Repository
public interface ActivityIncidentRepository extends JpaRepository<ActivityIncident, Long> {
    List<ActivityIncident> findByActivity(Activity activity);
    Optional<ActivityIncident> findByActivityAndOriginalDate(Activity activity, LocalDate date);
    List<ActivityIncident> findByActivityAndCancelledTrue(Activity activity);
}
