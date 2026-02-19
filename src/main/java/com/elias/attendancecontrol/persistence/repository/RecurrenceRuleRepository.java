package com.elias.attendancecontrol.persistence.repository;
import com.elias.attendancecontrol.model.entity.Activity;
import com.elias.attendancecontrol.model.entity.RecurrenceRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
@Repository
public interface RecurrenceRuleRepository extends JpaRepository<RecurrenceRule, Long> {
    Optional<RecurrenceRule> findByActivity(Activity activity);
    List<RecurrenceRule> findByEndDateAfterOrEndDateIsNull(LocalDate date);
}
