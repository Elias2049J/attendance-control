package com.elias.attendancecontrol.service.implementation;
import com.elias.attendancecontrol.model.entity.Activity;
import com.elias.attendancecontrol.model.entity.RecurrenceRule;
import com.elias.attendancecontrol.model.entity.RecurrenceType;
import com.elias.attendancecontrol.persistence.repository.ActivityRepository;
import com.elias.attendancecontrol.persistence.repository.RecurrenceRuleRepository;
import com.elias.attendancecontrol.service.LogService;
import com.elias.attendancecontrol.service.RecurrenceService;
import com.elias.attendancecontrol.service.SessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecurrenceServiceImpl implements RecurrenceService {
    private final RecurrenceRuleRepository recurrenceRuleRepository;
    private final ActivityRepository activityRepository;
    private final SessionService sessionService;
    private final LogService logService;

    @Override
    @Transactional
    public RecurrenceRule createRecurrenceRule(Long activityId, RecurrenceRule recurrenceRule) {
        return configureRecurrence(activityId, recurrenceRule);
    }

    @Override
    @Transactional
    public RecurrenceRule updateRecurrenceRule(Long ruleId, RecurrenceRule recurrenceRule) {
        return updateRecurrence(ruleId, recurrenceRule);
    }

    @Override
    @Transactional
    public void generateSessionsForActivity(Long activityId) {
        log.debug("Generating sessions for activity: {}", activityId);
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new IllegalArgumentException("Actividad no encontrada"));

        if (activity.getRecurrenceRule() == null) {
            throw new IllegalStateException("La actividad no tiene regla de recurrencia configurada");
        }

        sessionService.generateSessions(activityId);

        logService.log(builder -> builder
                .eventType("SESSIONS_GENERATED")
                .description("Sesiones generadas para actividad " + activity.getName())
                .details("Activity ID: " + activityId)
        );

        log.info("Sessions generated successfully for activity: {}", activityId);
    }

    @Override
    @Transactional
    public RecurrenceRule configureRecurrence(Long activityId, RecurrenceRule recurrenceRule) {
        log.debug("Configuring recurrence for activity: {}", activityId);
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new IllegalArgumentException("Actividad no encontrada"));

        if (!validateRecurrence(recurrenceRule)) {
            throw new IllegalArgumentException("Regla de recurrencia inválida");
        }

        if (activity.getRecurrenceRule() != null) {
            RecurrenceRule oldRule = activity.getRecurrenceRule();
            activity.setRecurrenceRule(null);
            activityRepository.save(activity);
            recurrenceRuleRepository.delete(oldRule);
            log.debug("Previous recurrence rule deleted for activity: {}", activityId);
        }

        recurrenceRule.setActivity(activity);
        RecurrenceRule savedRule = recurrenceRuleRepository.save(recurrenceRule);

        logService.log(builder -> builder
                .eventType("RECURRENCE_CONFIGURED")
                .description("Regla de recurrencia configurada para actividad " + activity.getName())
                .details("Tipo: " + savedRule.getRecurrenceType() + ", Inicio: " + savedRule.getStartDate())
        );

        log.info("Recurrence configured for activity {}: type={}", activityId, savedRule.getRecurrenceType());
        return savedRule;
    }

    @Override
    @Transactional
    public RecurrenceRule updateRecurrence(Long id, RecurrenceRule recurrenceRule) {
        log.debug("Updating recurrence rule: {}", id);
        RecurrenceRule existingRule = recurrenceRuleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Regla de recurrencia no encontrada"));

        if (!validateRecurrence(recurrenceRule)) {
            throw new IllegalArgumentException("Regla de recurrencia inválida");
        }

        existingRule.setRecurrenceType(recurrenceRule.getRecurrenceType());
        existingRule.setDaysOfWeek(recurrenceRule.getDaysOfWeek());
        existingRule.setStartDate(recurrenceRule.getStartDate());
        existingRule.setEndDate(recurrenceRule.getEndDate());
        existingRule.setStartTime(recurrenceRule.getStartTime());
        existingRule.setEndTime(recurrenceRule.getEndTime());
        existingRule.setToleranceMinutes(recurrenceRule.getToleranceMinutes());

        RecurrenceRule updatedRule = recurrenceRuleRepository.save(existingRule);

        logService.log(builder -> builder
                .eventType("RECURRENCE_UPDATED")
                .description("Regla de recurrencia actualizada")
                .details("ID: " + id + ", Tipo: " + updatedRule.getRecurrenceType())
        );

        log.info("Recurrence rule updated: {}", id);
        return updatedRule;
    }

    @Override
    public boolean validateRecurrence(RecurrenceRule rule) {
        if (rule == null) {
            log.debug("Validation failed: rule is null");
            return false;
        }
        if (rule.getStartDate() == null) {
            log.debug("Validation failed: start date is null");
            return false;
        }
        if (rule.getEndDate() != null && rule.getEndDate().isBefore(rule.getStartDate())) {
            log.debug("Validation failed: end date is before start date");
            return false;
        }
        if (rule.getRecurrenceType() == null) {
            log.debug("Validation failed: recurrence type is null");
            return false;
        }
        if (rule.getRecurrenceType() == RecurrenceType.WEEKLY &&
            (rule.getDaysOfWeek() == null || rule.getDaysOfWeek().isEmpty())) {
            log.debug("Validation failed: WEEKLY recurrence without days of week");
            return false;
        }
        if (rule.getStartTime() == null || rule.getEndTime() == null) {
            log.debug("Validation failed: start time or end time is null");
            return false;
        }
        if (rule.getEndTime().isBefore(rule.getStartTime())) {
            log.debug("Validation failed: end time is before start time");
            return false;
        }
        if (rule.getToleranceMinutes() == null || rule.getToleranceMinutes() < 0) {
            log.debug("Validation failed: tolerance minutes is null or negative");
            return false;
        }
        long sessionDuration = rule.getDurationMinutes();
        if (rule.getToleranceMinutes() > sessionDuration) {
            log.debug("Validation failed: tolerance ({} min) exceeds session duration ({} min)",
                rule.getToleranceMinutes(), sessionDuration);
            return false;
        }
        log.debug("Recurrence rule validation passed");
        return true;
    }
}

