package com.elias.attendancecontrol.service.implementation;
import com.elias.attendancecontrol.config.TenantContext;
import com.elias.attendancecontrol.model.entity.*;
import com.elias.attendancecontrol.persistence.repository.*;
import com.elias.attendancecontrol.service.LogService;
import com.elias.attendancecontrol.service.SessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
@Slf4j
@Service
@RequiredArgsConstructor
public class SessionServiceImpl implements SessionService {
    private final SessionRepository sessionRepository;
    private final ActivityRepository activityRepository;
    private final ActivityIncidentRepository activityIncidentRepository;
    private final QrTokenRepository qrTokenRepository;
    private final LogService logService;
    @Override
    @Transactional
    public Session activateSession(Long id) {
        log.debug("Attempting to activate session: {}", id);
        Session session = sessionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Sesión no encontrada"));
        validateSessionTolerance(session);
        if (session.getStatus() != SessionStatus.PLANNED) {
            throw new IllegalStateException(
                "Solo se pueden activar sesiones en estado PLANNED. Estado actual: " + session.getStatus());
        }
        LocalDate today = LocalDate.now();
        if (session.getSessionDate().isAfter(today.plusDays(1))) {
            throw new IllegalStateException(
                "No se puede activar una sesión con más de 1 día de anticipación");
        }
        session.setStatus(SessionStatus.ACTIVE);
        Session savedSession = sessionRepository.save(session);
        logService.log(builder -> builder
                .eventType("SESSION_ACTIVATED")
                .description("Sesión activada: " + session.getId())
                .session(session)
                .details("Fecha: " + session.getSessionDate())
        );
        log.info("Session activated successfully: {}", id);
        return savedSession;
    }
    @Override
    @Transactional
    public Session closeSession(Long id) {
        log.debug("Attempting to close session: {}", id);
        Session session = sessionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Sesión no encontrada"));
        if (session.getStatus() != SessionStatus.ACTIVE) {
            throw new IllegalStateException(
                "Solo se pueden cerrar sesiones en estado ACTIVE. Estado actual: " + session.getStatus());
        }
        session.setStatus(SessionStatus.CLOSED);
        qrTokenRepository.findBySessionAndActiveTrue(session)
            .ifPresent(qr -> {
                qr.setActive(false);
                qrTokenRepository.save(qr);
                log.debug("QR token invalidated for session: {}", id);
            });
        Session savedSession = sessionRepository.save(session);
        logService.log(builder -> builder
                .eventType("SESSION_CLOSED")
                .description("Sesión cerrada: " + session.getId())
                .session(session)
                .details("Asistencias registradas: " + session.getAttendances().size())
        );
        log.info("Session closed successfully: {}", id);
        return savedSession;
    }
    @Override
    @Transactional(readOnly = true)
    public List<Session> listSessions() {
        if (TenantContext.hasCurrentOrganization()) {
            Long orgId = TenantContext.getCurrentOrganizationId();
            log.debug("Listing sessions for organization: {}", orgId);
            return sessionRepository.findByActivityOrganizationId(orgId);
        }
        log.debug("No organization context, listing all sessions");
        return sessionRepository.findAll();
    }
    @Override
    @Transactional(readOnly = true)
    public Session getSessionById(Long id) {
        return sessionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Sesión no encontrada"));
    }
    @Override
    public boolean validateState(Session session, SessionStatus targetStatus) {
        if (session == null || targetStatus == null) {
            return false;
        }
        return switch (targetStatus) {
            case ACTIVE -> session.getStatus() == SessionStatus.PLANNED;
            case CLOSED -> session.getStatus() == SessionStatus.ACTIVE;
            case CANCELLED -> session.getStatus() == SessionStatus.PLANNED;
            default -> false;
        };
    }
    @Override
    @Transactional(readOnly = true)
    public boolean canActivate(Long sessionId) {
        try {
            Session session = getSessionById(sessionId);
            LocalDate today = LocalDate.now();
            return session.getStatus() == SessionStatus.PLANNED
                && !session.getSessionDate().isAfter(today.plusDays(1));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
    @Override
    @Transactional(readOnly = true)
    public boolean canClose(Long sessionId) {
        try {
            Session session = getSessionById(sessionId);
            return session.getStatus() == SessionStatus.ACTIVE;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
    @Override
    @Transactional
    public List<Session> generateSessions(Long activityId) {
        log.debug("Generating sessions for activity: {}", activityId);
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new IllegalArgumentException("Actividad no encontrada"));
        RecurrenceRule rule = activity.getRecurrenceRule();
        if (rule == null) {
            throw new IllegalStateException("La actividad no tiene regla de recurrencia");
        }
        List<Session> sessions = new ArrayList<>();
        LocalDate currentDate = rule.getStartDate();
        LocalDate endDate = rule.getEndDate() != null
            ? rule.getEndDate()
            : currentDate.plusMonths(3);
        while (!currentDate.isAfter(endDate)) {
            if (shouldGenerateSession(currentDate, rule)) {
                if (!sessionRepository.existsByActivityAndSessionDate(activity, currentDate)) {
                    Session session = createSessionForDate(activity, currentDate, rule);
                    sessions.add(sessionRepository.save(session));
                    log.debug("Session created for date: {}", currentDate);
                }
            }
            currentDate = getNextDate(currentDate, rule.getRecurrenceType());
        }
        sessions = applyExceptions(activityId, sessions);
        final List<Session> generatedSessions = sessions;
        if (!generatedSessions.isEmpty()) {
            logService.log(builder -> builder
                    .eventType("SESSIONS_GENERATED")
                    .description(generatedSessions.size() + " sesiones generadas")
                    .session(generatedSessions.getFirst())
                    .details("Período: " + rule.getStartDate() + " a " + endDate)
            );
        }
        log.info("Generated {} sessions for activity: {}", generatedSessions.size(), activityId);
        return generatedSessions;
    }
    @Override
    @Transactional(readOnly = true)
    public List<Session> applyRecurrenceRules(Long activityId) {
        return generateSessions(activityId);
    }
    @Override
    @Transactional(readOnly = true)
    public List<Session> applyExceptions(Long activityId, List<Session> sessions) {
        log.debug("Applying exceptions for activity: {}", activityId);
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new IllegalArgumentException("Actividad no encontrada"));
        List<ActivityIncident> incidents = activityIncidentRepository.findByActivity(activity);
        if (incidents.isEmpty()) {
            return sessions;
        }
        List<Session> result = new ArrayList<>();
        for (Session session : sessions) {
            boolean shouldSkip = false;
            for (ActivityIncident incident : incidents) {
                if (incident.getOriginalDate().equals(session.getSessionDate())) {
                    if (incident.getCancelled()) {
                        session.setStatus(SessionStatus.CANCELLED);
                        sessionRepository.save(session);
                        shouldSkip = true;
                        log.debug("Session cancelled for date: {}", session.getSessionDate());
                        break;
                    } else if (incident.getNewDate() != null) {
                        session.setSessionDate(incident.getNewDate());
                        sessionRepository.save(session);
                        log.debug("Session rescheduled from {} to {}",
                            incident.getOriginalDate(), incident.getNewDate());
                    }
                }
            }
            if (!shouldSkip) {
                result.add(session);
            }
        }
        log.debug("Applied {} exceptions, {} sessions remain", incidents.size(), result.size());
        return result;
    }
    private boolean shouldGenerateSession(LocalDate date, RecurrenceRule rule) {
        return switch (rule.getRecurrenceType()) {
            case DAILY -> true;
            case WEEKLY -> isInWeekDays(date, rule.getDaysOfWeek());
            case MONTHLY -> date.getDayOfMonth() == rule.getStartDate().getDayOfMonth();
            default -> false;
        };
    }
    private boolean isInWeekDays(LocalDate date, String daysOfWeek) {
        if (daysOfWeek == null || daysOfWeek.isEmpty()) {
            return false;
        }
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        String dayName = dayOfWeek.toString();
        return daysOfWeek.toUpperCase().contains(dayName);
    }
    private Session createSessionForDate(Activity activity, LocalDate date, RecurrenceRule rule) {
        Session session = new Session();
        session.setActivity(activity);
        session.setSessionDate(date);
        session.setStartTime(rule.getStartTime());
        session.setEndTime(rule.getEndTime());
        session.setToleranceMinutes(rule.getToleranceMinutes());
        session.setStatus(SessionStatus.PLANNED);
        return session;
    }
    private LocalDate getNextDate(LocalDate current, RecurrenceType type) {
        return switch (type) {
            case DAILY -> current.plusDays(1);
            case WEEKLY -> current.plusWeeks(1);
            case MONTHLY -> current.plusMonths(1);
            default -> current.plusDays(1);
        };
    }
    private void validateSessionTolerance(Session session) {
        if (session.getToleranceMinutes() == null) {
            throw new IllegalStateException("La sesión no tiene tolerancia configurada");
        }
        if (session.getToleranceMinutes() < 0) {
            throw new IllegalStateException("La tolerancia no puede ser negativa");
        }
        long sessionDuration = session.getDurationMinutes();
        if (session.getToleranceMinutes() > sessionDuration) {
            throw new IllegalStateException(
                String.format("La tolerancia (%d min) no puede exceder la duración de la sesión (%d min)",
                    session.getToleranceMinutes(), sessionDuration));
        }
        log.debug("Session tolerance validated: tolerance={} min, duration={} min",
            session.getToleranceMinutes(), sessionDuration);
    }
    @Override
    @Transactional(readOnly = true)
    public List<Session> getSessionsByActivity(Long activityId) {
        log.debug("Getting sessions for activity: {}", activityId);
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new IllegalArgumentException("Actividad no encontrada"));
        return sessionRepository.findByActivity(activity);
    }
}
