package com.elias.attendancecontrol.service.implementation;
import com.elias.attendancecontrol.config.TenantContext;
import com.elias.attendancecontrol.model.entity.Activity;
import com.elias.attendancecontrol.model.entity.ActivityStatus;
import com.elias.attendancecontrol.model.entity.Organization;
import com.elias.attendancecontrol.model.entity.SessionStatus;
import com.elias.attendancecontrol.persistence.repository.ActivityRepository;
import com.elias.attendancecontrol.persistence.repository.OrganizationRepository;
import com.elias.attendancecontrol.persistence.repository.SessionRepository;
import com.elias.attendancecontrol.service.ActivityService;
import com.elias.attendancecontrol.service.EnrollmentService;
import com.elias.attendancecontrol.service.LogService;
import com.elias.attendancecontrol.service.OrganizationService;
import com.elias.attendancecontrol.service.RecurrenceService;
import com.elias.attendancecontrol.service.SessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityServiceImpl implements ActivityService {
    private final ActivityRepository activityRepository;
    private final OrganizationRepository organizationRepository;
    private final SessionRepository sessionRepository;
    private final OrganizationService organizationService;
    private final SessionService sessionService;
    private final EnrollmentService enrollmentService;
    private final RecurrenceService recurrenceService;
    private final LogService logService;
    @Override
    @Transactional
    public Activity createActivity(Activity activity) {
        log.debug("Creating new activity: {}", activity.getName());
        if (TenantContext.hasCurrentOrganization()) {
            Long orgId = TenantContext.getCurrentOrganizationId();
            Organization organization = organizationRepository.findById(orgId)
                    .orElseThrow(() -> new IllegalArgumentException("Organización no encontrada"));
            if (!organizationService.canAddActivity(orgId)) {
                throw new IllegalStateException(
                        "Has alcanzado el límite de actividades de tu plan (" +
                        organization.getMaxActivities() + " actividades)");
            }
            activity.setOrganization(organization);
        }
        Activity savedActivity = activityRepository.save(activity);
        logService.log(builder -> builder
                .eventType("ACTIVITY_CREATED")
                .description("Actividad creada: " + savedActivity.getName())
                .details("ID: " + savedActivity.getId())
        );
        log.info("Activity created successfully: {}", savedActivity.getName());
        return savedActivity;
    }
    @Override
    @Transactional
    public Activity updateActivity(Long id, Activity activity) {
        log.debug("Updating activity: {}", id);
        Activity existingActivity = activityRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Actividad no encontrada"));
        existingActivity.setName(activity.getName());
        existingActivity.setDescription(activity.getDescription());
        existingActivity.setResponsible(activity.getResponsible());
        Activity updatedActivity = activityRepository.save(existingActivity);
        logService.log(builder -> builder
                .eventType("ACTIVITY_UPDATED")
                .description("Actividad actualizada: " + updatedActivity.getName())
                .details("ID: " + id)
        );
        log.info("Activity updated successfully: {}", id);
        return updatedActivity;
    }
    @Override
    @Transactional
    public void activateActivity(Long id) {
        log.debug("Activating activity: {}", id);
        Activity activity = activityRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Actividad no encontrada"));

        if (activity.getRecurrenceRule() == null) {
            throw new IllegalStateException("La actividad no tiene regla de recurrencia configurada");
        }

        if (activity.getStatus() == ActivityStatus.DRAFT) {
            activity.setStatus(ActivityStatus.SCHEDULED);
            activityRepository.save(activity);

            recurrenceService.generateSessionsForActivity(id);

            logService.log(builder -> builder
                    .eventType("ACTIVITY_ACTIVATED")
                    .description("Actividad activada y sesiones generadas: " + activity.getName())
                    .details("ID: " + id + ", Estado: " + activity.getStatus())
            );
            log.info("Activity activated successfully and sessions generated: {}", id);
        } else {
            throw new IllegalStateException("Solo se pueden activar actividades en estado Borrador. Estado actual: " + activity.getStatus().getDisplayName());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Activity> listActivities() {
        if (TenantContext.hasCurrentOrganization()) {
            Long orgId = TenantContext.getCurrentOrganizationId();
            return activityRepository.findByOrganizationId(orgId);
        }
        return activityRepository.findAll();
    }
    @Override
    @Transactional(readOnly = true)
    public Activity getActivityById(Long id) {
        return activityRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Actividad no encontrada"));
    }
    @Transactional(readOnly = true)
    @Override
    public List<Activity> findActiveActivities() {
        if (TenantContext.hasCurrentOrganization()) {
            Long orgId = TenantContext.getCurrentOrganizationId();
            return activityRepository.findByOrganizationIdAndStatus(orgId, ActivityStatus.SCHEDULED);
        }
        return activityRepository.findByStatus(ActivityStatus.SCHEDULED);
    }
    @Transactional(readOnly = true)
    @Override
    public List<Activity> findByResponsible(Long userId) {
        return activityRepository.findByResponsibleId(userId);
    }

    @Override
    @Transactional
    public void pauseActivity(Long activityId) {
        log.debug("Pausing activity: {}", activityId);
        Activity activity = getActivityById(activityId);
        if (activity.getStatus() != ActivityStatus.SCHEDULED) {
            throw new IllegalStateException("Solo se pueden pausar actividades programadas");
        }
        activity.setStatus(ActivityStatus.PAUSED);
        activityRepository.save(activity);
        logService.log(builder -> builder
                .eventType("ACTIVITY_PAUSED")
                .description("Actividad pausada: " + activity.getName())
        );
        log.info("Activity paused successfully: {}", activityId);
    }
    @Override
    @Transactional
    public void completeActivity(Long activityId) {
        log.debug("Completing activity: {}", activityId);
        Activity activity = getActivityById(activityId);
        if (activity.getStatus().isFinalState()) {
            throw new IllegalStateException("La actividad ya está en estado final");
        }
        long pendingSessions = sessionRepository.countByActivityAndStatus(activity, SessionStatus.PLANNED);
        if (pendingSessions > 0) {
            throw new IllegalStateException("Aún hay sesiones pendientes por realizar");
        }
        long activeSessions = sessionRepository.countByActivityAndStatus(activity, SessionStatus.ACTIVE);
        if (activeSessions > 0) {
            throw new IllegalStateException("Hay sesiones activas en este momento");
        }
        activity.setStatus(ActivityStatus.COMPLETED);
        activityRepository.save(activity);
        enrollmentService.completeAllEnrollments(activityId);
        logService.log(builder -> builder
                .eventType("ACTIVITY_COMPLETED")
                .description("Actividad completada: " + activity.getName())
        );
        log.info("Activity completed successfully: {}", activityId);
    }
    @Override
    @Transactional
    public void cancelActivity(Long activityId) {
        log.debug("Cancelling activity: {}", activityId);
        Activity activity = getActivityById(activityId);
        if (activity.getStatus().isFinalState()) {
            throw new IllegalStateException("La actividad ya está en estado final");
        }
        activity.setStatus(ActivityStatus.CANCELLED);
        activityRepository.save(activity);
        logService.log(builder -> builder
                .eventType("ACTIVITY_CANCELLED")
                .description("Actividad cancelada: " + activity.getName())
        );
        log.info("Activity cancelled successfully: {}", activityId);
    }
    @Override
    @Transactional(readOnly = true)
    public boolean canPublish(Long activityId) {
        try {
            Activity activity = getActivityById(activityId);
            return activity.getStatus() == ActivityStatus.DRAFT
                    && activity.getRecurrenceRule() != null;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
    @Override
    @Transactional(readOnly = true)
    public boolean canComplete(Long activityId) {
        try {
            Activity activity = getActivityById(activityId);
            if (activity.getStatus().isFinalState()) {
                return false;
            }
            long pendingSessions = sessionRepository.countByActivityAndStatus(activity, SessionStatus.PLANNED);
            long activeSessions = sessionRepository.countByActivityAndStatus(activity, SessionStatus.ACTIVE);
            return pendingSessions == 0 && activeSessions == 0;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
    @Override
    @Transactional
    public void changeStatus(Long activityId, ActivityStatus newStatus) {
        log.debug("Changing status for activity {} to {}", activityId, newStatus);
        Activity activity = getActivityById(activityId);
        ActivityStatus oldStatus = activity.getStatus();
        validateStatusTransition(oldStatus, newStatus);
        activity.setStatus(newStatus);
        activityRepository.save(activity);
        logService.log(builder -> builder
                .eventType("ACTIVITY_STATUS_CHANGED")
                .description("Estado de actividad cambiado: " + activity.getName())
                .details("De " + oldStatus + " a " + newStatus)
        );
        log.info("Activity status changed: {} -> {}", oldStatus, newStatus);
    }
    private void validateStatusTransition(ActivityStatus current, ActivityStatus target) {
        if (current == target) {
            throw new IllegalStateException("La actividad ya está en ese estado");
        }
        if (current.isFinalState()) {
            throw new IllegalStateException("No se puede cambiar el estado de una actividad finalizada");
        }
        boolean validTransition = switch (current) {
            case DRAFT -> target == ActivityStatus.SCHEDULED || target == ActivityStatus.CANCELLED;
            case SCHEDULED -> target == ActivityStatus.PAUSED || target == ActivityStatus.COMPLETED || target == ActivityStatus.CANCELLED;
            case PAUSED -> target == ActivityStatus.SCHEDULED || target == ActivityStatus.CANCELLED;
            default -> false;
        };
        if (!validTransition) {
            throw new IllegalStateException("Transición de estado inválida: " + current + " -> " + target);
        }
    }
    @Override
    @Transactional(readOnly = true)
    public List<Activity> searchActivities(String query, Long userId, String role) {
        log.debug("Searching activities with query: {}, userId: {}, role: {}", query, userId, role);
        List<Activity> activities;
        if (role.contains("ORG_MEMBER")) {
            activities = enrollmentService.getActivitiesByUser(userId);
        } else if (role.contains("RESPONSIBLE")) {
            activities = findByResponsible(userId);
        } else {
            activities = listActivities();
        }
        if (query != null && !query.isBlank()) {
            String lowerQuery = query.toLowerCase();
            activities = activities.stream()
                    .filter(a -> a.getName().toLowerCase().contains(lowerQuery) ||
                            (a.getDescription() != null && a.getDescription().toLowerCase().contains(lowerQuery)))
                    .toList();
        }
        return activities;
    }
}
