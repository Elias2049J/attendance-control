package com.elias.attendancecontrol.service.implementation;
import com.elias.attendancecontrol.config.SecurityUtils;
import com.elias.attendancecontrol.model.entity.Activity;
import com.elias.attendancecontrol.model.entity.RecurrenceRule;
import com.elias.attendancecontrol.persistence.repository.ActivityRepository;
import com.elias.attendancecontrol.persistence.repository.RecurrenceRuleRepository;
import com.elias.attendancecontrol.service.LogService;
import com.elias.attendancecontrol.service.RecurrenceService;
import com.elias.attendancecontrol.service.SessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecurrenceServiceImpl implements RecurrenceService {
    private final RecurrenceRuleRepository recurrenceRuleRepository;
    private final ActivityRepository activityRepository;
    private final SessionService sessionService;
    private final LogService logService;
    private final SecurityUtils securityUtils;

    @Override
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
    public RecurrenceRule configureRecurrence(Long activityId, RecurrenceRule recurrenceRule) {
        log.debug("Configuring recurrence for activity: {}", activityId);
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new IllegalArgumentException("Actividad no encontrada"));

        validateRecurrence(recurrenceRule);

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
    public RecurrenceRule updateRecurrence(Long id, RecurrenceRule recurrenceRule) {
        log.debug("Updating recurrence rule: {}", id);
        RecurrenceRule existingRule = recurrenceRuleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Regla de recurrencia no encontrada"));

        validateRecurrence(recurrenceRule);

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
                .organization(securityUtils.getCurrentOrganization().orElse(null))
        );

        log.info("Recurrence rule updated: {}", id);
        return updatedRule;
    }

    @Override
    public boolean validateRecurrence(RecurrenceRule rule) {
        if (rule == null) {
            throw new IllegalArgumentException("La regla de recurrencia no puede ser nula");
        }
        if (rule.getStartDate() == null) {
            throw new IllegalArgumentException("La fecha de inicio es obligatoria");
        }
        if (rule.getStartDate().isBefore(java.time.LocalDate.now())) {
            throw new IllegalArgumentException("La fecha de inicio no puede ser anterior a hoy");
        }
        if (rule.getEndDate() != null && rule.getEndDate().isBefore(rule.getStartDate())) {
            throw new IllegalArgumentException("La fecha de fin no puede ser anterior a la fecha de inicio");
        }
        if (rule.getRecurrenceType() == null) {
            throw new IllegalArgumentException("El tipo de recurrencia es obligatorio");
        }
        if (rule.getRecurrenceType().isWeekly() &&
            (rule.getDaysOfWeek() == null || rule.getDaysOfWeek().isEmpty())) {
            throw new IllegalArgumentException("Debe seleccionar al menos un día de la semana para la recurrencia semanal");
        }
        if (rule.getStartTime() == null || rule.getEndTime() == null) {
            throw new IllegalArgumentException("La hora de inicio y fin son obligatorias");
        }
        if (rule.getEndTime().isBefore(rule.getStartTime()) || rule.getEndTime().equals(rule.getStartTime())) {
            throw new IllegalArgumentException("La hora de fin debe ser posterior a la hora de inicio");
        }
        if (rule.getToleranceMinutes() == null || rule.getToleranceMinutes() < 0) {
            throw new IllegalArgumentException("La tolerancia no puede ser negativa");
        }
        long sessionDuration = rule.getDurationMinutes();
        if (rule.getToleranceMinutes() > sessionDuration) {
            throw new IllegalArgumentException(
                "La tolerancia (" + rule.getToleranceMinutes() + " min) no puede superar la duración de la sesión (" + sessionDuration + " min)");
        }
        log.debug("Recurrence rule validation passed");
        return true;
    }
}

