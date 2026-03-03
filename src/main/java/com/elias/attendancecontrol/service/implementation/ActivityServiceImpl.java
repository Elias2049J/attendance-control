package com.elias.attendancecontrol.service.implementation;

import com.elias.attendancecontrol.config.SecurityUtils;
import com.elias.attendancecontrol.model.entity.*;
import com.elias.attendancecontrol.persistence.repository.ActivityRepository;
import com.elias.attendancecontrol.persistence.repository.SessionRepository;
import com.elias.attendancecontrol.service.ActivityService;
import com.elias.attendancecontrol.service.EnrollmentService;
import com.elias.attendancecontrol.service.LogService;
import com.elias.attendancecontrol.service.OrganizationService;
import com.elias.attendancecontrol.service.RecurrenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityServiceImpl implements ActivityService {
    private final ActivityRepository activityRepository;
    private final SessionRepository sessionRepository;
    private final OrganizationService organizationService;
    private final EnrollmentService enrollmentService;
    private final RecurrenceService recurrenceService;
    private final LogService logService;
    private final SecurityUtils securityUtils;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Activity createActivityWithRecurrence(Activity activity, RecurrenceRule recurrenceRule) {
        log.debug("Creating activity with recurrence: {}", activity.getName());
        Activity savedActivity = createActivity(activity);
        recurrenceService.configureRecurrence(savedActivity.getId(), recurrenceRule);
        log.info("Activity with recurrence created successfully: {}", savedActivity.getId());
        return savedActivity;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Activity updateActivityWithRecurrence(Long id, Activity activity, RecurrenceRule recurrenceRule) {
        log.debug("Updating activity with recurrence: {}", id);
        Activity updated = updateActivity(id, activity);
        if (updated.getRecurrenceRule() != null) {
            recurrenceService.updateRecurrence(updated.getRecurrenceRule().getId(), recurrenceRule);
        } else {
            recurrenceService.configureRecurrence(id, recurrenceRule);
        }
        log.info("Activity with recurrence updated successfully: {}", id);
        return updated;
    }

    @Override
    public Activity createActivity(Activity activity) {
        log.debug("Creating new activity: {}", activity.getName());

        Organization organization = securityUtils.getCurrentOrganization().orElseThrow();
            if (!organizationService.canAddActivity(organization.getId())) {
                throw new IllegalStateException(
                        "Has alcanzado el límite de actividades de tu plan (" +
                        organization.getMaxActivities() + " actividades)");
            }
        activity.setOrganization(organization);

        Activity savedActivity = activityRepository.save(activity);

        securityUtils.getCurrentUser().ifPresentOrElse(
            currentUser -> logService.log(builder -> builder
                    .eventType("ACTIVITY_CREATED")
                    .description("Actividad creada: " + savedActivity.getName())
                    .user(currentUser)
                    .organization(organization)
                    .details("ID: " + savedActivity.getId())
            ),
            () -> logService.log(builder -> builder
                    .eventType("ACTIVITY_CREATED")
                    .organization(organization)
                    .description("Actividad creada: " + savedActivity.getName())
                    .details("ID: " + savedActivity.getId())
            )
        );

        log.info("Activity created successfully: {}", savedActivity.getName());
        return savedActivity;
    }

    @Override
    public Activity updateActivity(Long id, Activity activity) {
        log.debug("Updating activity: {}", id);
        Activity existingActivity = getActivityById(id);
        existingActivity.setDescription(activity.getDescription());
        if (activity.getResponsible() != null) existingActivity.setResponsible(activity.getResponsible());
        Activity updatedActivity = activityRepository.save(existingActivity);

        securityUtils.getCurrentUser().ifPresent(currentUser ->
            logService.log(builder -> builder
                    .eventType("ACTIVITY_UPDATED")
                    .description("Actividad actualizada: " + updatedActivity.getName())
                    .user(currentUser)
                    .organization(currentUser.getOrganization())
                    .details("ID: " + id)
            )
        );

        log.info("Activity updated successfully: {}", id);
        return updatedActivity;
    }

    @Override
    @Transactional
    public void activateActivity(Long id) {
        log.debug("Activating activity: {}", id);
        Activity activity = getActivityById(id);

        if (activity.getRecurrenceRule() == null) {
            throw new IllegalStateException("La actividad no tiene regla de recurrencia configurada");
        }

        if (activity.getStatus().isDraft()) {
            activity.setStatus(ActivityStatus.SCHEDULED);
            activityRepository.save(activity);

            recurrenceService.generateSessionsForActivity(id);

            securityUtils.getCurrentUser().ifPresent(currentUser ->
                logService.log(builder -> builder
                        .eventType("ACTIVITY_ACTIVATED")
                        .description("Actividad activada y sesiones generadas: " + activity.getName())
                        .user(currentUser)
                        .organization(currentUser.getOrganization())
                        .details("ID: " + id + ", Estado: " + activity.getStatus().getDisplayName())
                )
            );

            log.info("Activity activated successfully and sessions generated: {}", id);
        } else {
            throw new IllegalStateException("Solo se pueden activar actividades en estado Borrador. Estado actual: " + activity.getStatus().getDisplayName());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Activity> listActivitiesSorted() {
        return securityUtils.getCurrentOrganizationId()
                .map(activityRepository::findByOrganizationIdWithDetails)
                .orElseGet(() -> activityRepository.findAll()
                        .stream()
                        .sorted(Comparator.comparing((Activity a) ->
                                        a.getRecurrenceRule() != null
                                                ? a.getRecurrenceRule().getStartDate()
                                                : LocalDate.MAX)
                                .thenComparing(Activity::getId, Comparator.reverseOrder())
                        )
                        .toList()
                );
    }

    @Override
    @Transactional(readOnly = true)
    public Activity getActivityById(Long id) {
        Activity activity = activityRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new IllegalArgumentException("Actividad no encontrada"));
        if (activity.getOrganization() != null) {
            securityUtils.validateResourceOwnership(activity.getOrganization().getId());
        }
        return activity;
    }

    @Transactional(readOnly = true)
    @Override
    public List<Activity> findActiveActivities() {
        return securityUtils.getCurrentOrganizationId()
                .map(orgId -> activityRepository.findByOrganizationIdAndStatus(orgId, ActivityStatus.SCHEDULED))
                .orElseGet(() -> activityRepository.findByStatusOrderByIdDesc(ActivityStatus.SCHEDULED));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Activity> findByResponsible(Long userId) {
        return activityRepository.findByResponsibleIdWithDetails(userId)
                .stream()
                .filter(a -> !a.getStatus().isDraft())
                .sorted(Comparator.comparing(
                        (Activity a) -> a.getRecurrenceRule() != null
                                ? a.getRecurrenceRule().getStartDate()
                                : LocalDate.MAX)
                        .thenComparing(Activity::getId, Comparator.reverseOrder())
                ).toList();
    }

    @Override
    @Transactional
    public void pauseActivity(Long activityId) {
        log.debug("Pausing activity: {}", activityId);
        Activity activity = getActivityById(activityId);
        if (!activity.getStatus().isScheduled()) {
            throw new IllegalStateException("Solo se pueden pausar actividades programadas");
        }
        activity.setStatus(ActivityStatus.PAUSED);
        activityRepository.save(activity);

        securityUtils.getCurrentUser().ifPresent(currentUser ->
            logService.log(builder -> builder
                    .eventType("ACTIVITY_PAUSED")
                    .description("Actividad pausada: " + activity.getName())
                    .user(currentUser)
            )
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

        securityUtils.getCurrentUser().ifPresent(currentUser ->
            logService.log(builder -> builder
                    .eventType("ACTIVITY_COMPLETED")
                    .description("Actividad completada: " + activity.getName())
                    .user(currentUser)
                    .organization(currentUser.getOrganization())
            )
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
        List<Session> sessions = sessionRepository.findByActivityAndStatus(activity, SessionStatus.PLANNED);
        sessions.addAll(sessionRepository.findByActivityAndStatus(activity, SessionStatus.ACTIVE));
        sessions.forEach(s -> s.setStatus(SessionStatus.CANCELLED));
        sessionRepository.saveAll(sessions);
        log.debug("Cancelled {} sessions for activity: {}", sessions.size(), activityId);
        activity.setStatus(ActivityStatus.CANCELLED);
        activityRepository.save(activity);

        securityUtils.getCurrentUser().ifPresent(currentUser ->
            logService.log(builder -> builder
                    .eventType("ACTIVITY_CANCELLED")
                    .description("Actividad cancelada: " + activity.getName() + " (" + sessions.size() + " sesiones canceladas)")
                    .user(currentUser)
                    .organization(currentUser.getOrganization())
            )
        );

        log.info("Activity cancelled successfully: {}", activityId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canPublish(Long activityId) {
        try {
            Activity activity = getActivityById(activityId);
            return activity.getStatus().isDraft()
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

        securityUtils.getCurrentUser().ifPresent(currentUser ->
            logService.log(builder -> builder
                    .eventType("ACTIVITY_STATUS_CHANGED")
                    .description("Estado de actividad cambiado: " + activity.getName())
                    .user(currentUser)
                    .organization(currentUser.getOrganization())
                    .details("De " + oldStatus + " a " + newStatus)
            )
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
            activities = listActivitiesSorted();
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

    @Override
    @Transactional(readOnly = true)
    public boolean isResponsible(Long activityId, Long userId) {
        long responsibleId = getActivityById(activityId)
                .getResponsible().getId();
        return responsibleId == userId;
    }

    @Override
    public long countAll() {
        return activityRepository.count();
    }

    @Override
    public List<Activity> findAllByUserResponsibleAndEnrolled(long userId) {
        List<Activity> enrolled = enrollmentService.getActivitiesByUser(userId);
        List<Activity> responsible = findByResponsible(userId);
        Set<Activity> filteredByEnrolledAndResponsible = new LinkedHashSet<>();

        filteredByEnrolledAndResponsible.addAll(enrolled);
        filteredByEnrolledAndResponsible.addAll(responsible);
        return filteredByEnrolledAndResponsible
                .stream()
                .sorted(Comparator.comparing((Activity a) -> a.getRecurrenceRule().getStartDate())
                        .thenComparing(Activity::getId, Comparator.reverseOrder()))
                .toList();
    }
}
