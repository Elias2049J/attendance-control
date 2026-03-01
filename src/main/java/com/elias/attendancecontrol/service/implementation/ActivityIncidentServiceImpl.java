package com.elias.attendancecontrol.service.implementation;
import com.elias.attendancecontrol.config.SecurityUtils;
import com.elias.attendancecontrol.model.entity.Activity;
import com.elias.attendancecontrol.model.entity.ActivityIncident;
import com.elias.attendancecontrol.model.entity.Session;
import com.elias.attendancecontrol.model.entity.SessionStatus;
import com.elias.attendancecontrol.model.entity.User;
import com.elias.attendancecontrol.persistence.repository.ActivityIncidentRepository;
import com.elias.attendancecontrol.persistence.repository.AttendanceRepository;
import com.elias.attendancecontrol.persistence.repository.SessionRepository;
import com.elias.attendancecontrol.service.ActivityIncidentService;
import com.elias.attendancecontrol.service.ActivityService;
import com.elias.attendancecontrol.service.LogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;
@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityIncidentServiceImpl implements ActivityIncidentService {
    private final ActivityIncidentRepository activityIncidentRepository;
    private final SessionRepository sessionRepository;
    private final AttendanceRepository attendanceRepository;
    private final LogService logService;
    private final SecurityUtils securityUtils;
    private final ActivityService activityService;

    @Override
    @Transactional
    public ActivityIncident registerIncident(ActivityIncident incident) {
        log.debug("Registering incident for activity: {}", incident.getActivity().getId());
        if (incident.getActivity() == null) {
            throw new IllegalArgumentException("La actividad es obligatoria");
        }
        if (incident.getOriginalDate() == null) {
            throw new IllegalArgumentException("La fecha original es obligatoria");
        }
        ActivityIncident savedIncident = activityIncidentRepository.save(incident);
        log.info("Incident registered: id={}, activity={}",
            savedIncident.getId(), savedIncident.getActivity().getId());
        return savedIncident;
    }
    @Override
    @Transactional
    public ActivityIncident rescheduleOccurrence(Long activityId, LocalDate originalDate,
                                                 LocalDate newDate, String reason) {
        log.debug("Rescheduling occurrence for activity {} from {} to {}",
            activityId, originalDate, newDate);
        Activity activity = activityService.getActivityById(activityId);
        validateUserCanManageIncidents(activity);
        if (newDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("La nueva fecha no puede estar en el pasado");
        }
        Session originalSession = sessionRepository
            .findByActivityAndSessionDate(activity, originalDate)
            .orElseThrow(() -> new IllegalArgumentException(
                "No existe una sesión para la fecha original especificada"));
        if (sessionRepository.existsByActivityAndSessionDate(activity, newDate)) {
            throw new IllegalStateException("Ya existe una sesión programada para la nueva fecha");
        }
        if (originalSession.getStatus().isClosed()) {
            throw new IllegalStateException("No se puede reprogramar una sesión ya cerrada");
        }
        if (attendanceRepository.existsBySession(originalSession)) {
            throw new IllegalStateException(
                "No se puede reprogramar una sesión que ya tiene asistencias registradas");
        }
        ActivityIncident incident = new ActivityIncident();
        incident.setActivity(activity);
        incident.setOriginalDate(originalDate);
        incident.setNewDate(newDate);
        incident.setCancelled(false);
        incident.setReason(reason);
        incident.setCreatedByUser(getCurrentUser(activity));
        ActivityIncident savedIncident = activityIncidentRepository.save(incident);
        originalSession.setSessionDate(newDate);
        sessionRepository.save(originalSession);
        logService.log(builder -> builder
                .eventType("OCCURRENCE_RESCHEDULED")
                .description("Ocurrencia reprogramada")
                .organization(securityUtils.getCurrentOrganization().orElse(null))
                .session(originalSession)
                .details("De: " + originalDate + " a: " + newDate + " | Razón: " + reason)
        );
        log.info("Occurrence rescheduled: activity={}, from={}, to={}",
            activityId, originalDate, newDate);
        return savedIncident;
    }
    @Override
    @Transactional
    public ActivityIncident cancelOccurrence(Long activityId, LocalDate date, String reason) {
        log.debug("Cancelling occurrence for activity {} on date {}", activityId, date);
        Activity activity = activityService.getActivityById(activityId);
        validateUserCanManageIncidents(activity);
        Session session = sessionRepository
            .findByActivityAndSessionDate(activity, date)
            .orElseThrow(() -> new IllegalArgumentException(
                "No existe una sesión para la fecha especificada"));
        if (session.getStatus() == SessionStatus.CLOSED) {
            throw new IllegalStateException("No se puede cancelar una sesión ya cerrada");
        }
        if (attendanceRepository.existsBySession(session)) {
            throw new IllegalStateException(
                "No se puede cancelar una sesión que ya tiene asistencias registradas");
        }
        ActivityIncident incident = new ActivityIncident();
        incident.setActivity(activity);
        incident.setOriginalDate(date);
        incident.setNewDate(null);
        incident.setCancelled(true);
        incident.setReason(reason);
        incident.setCreatedByUser(getCurrentUser(activity));
        ActivityIncident savedIncident = activityIncidentRepository.save(incident);
        session.setStatus(SessionStatus.CANCELLED);
        sessionRepository.save(session);
        logService.log(builder -> builder
                .eventType("OCCURRENCE_CANCELLED")
                .description("Ocurrencia cancelada")
                .user(securityUtils.getCurrentUser().orElse(null))
                .organization(securityUtils.getCurrentOrganization().orElse(null))
                .session(session)
                .details("Fecha: " + date + " | Razón: " + reason)
        );
        log.info("Occurrence cancelled: activity={}, date={}", activityId, date);
        return savedIncident;
    }
    @Override
    @Transactional(readOnly = true)
    public List<ActivityIncident> getAllIncidents() {
        log.debug("Getting all incidents");
        return activityIncidentRepository.findAll();
    }
    @Override
    @Transactional(readOnly = true)
    public List<ActivityIncident> getIncidentsByActivity(Long activityId) {
        log.debug("Getting incidents for activity: {}", activityId);
        Activity activity = activityService.getActivityById(activityId);
        return activityIncidentRepository.findByActivity(activity);
    }
    @Override
    @Transactional(readOnly = true)
    public ActivityIncident getIncidentById(Long id) {
        log.debug("Getting incident by id: {}", id);
        return activityIncidentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Incidencia no encontrada"));
    }
    private void validateUserCanManageIncidents(Activity activity) {
        User currentUser = securityUtils.getCurrentUser()
                .orElseThrow(() -> new SecurityException("Usuario no autenticado"));
        boolean isSystemAdmin = securityUtils.isSystemAdmin();
        boolean isOrgOwner = securityUtils.isOrganizationOwner();
        boolean isOrgAdmin = securityUtils.isOrganizationAdmin();
        boolean isResponsible = activity.getResponsible() != null &&
                               activity.getResponsible().getId().equals(currentUser.getId());
        if (!isSystemAdmin && !isOrgOwner && !isOrgAdmin && !isResponsible) {
            throw new SecurityException(
                "Solo administradores o el responsable de la actividad pueden gestionar incidencias");
        }
    }
    private User getCurrentUser(Activity activity) {
        return securityUtils.getCurrentUser()
                .orElse(activity.getResponsible());
    }
}
