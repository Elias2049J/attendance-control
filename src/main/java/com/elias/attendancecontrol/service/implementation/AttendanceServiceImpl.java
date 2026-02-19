package com.elias.attendancecontrol.service.implementation;
import com.elias.attendancecontrol.model.entity.*;
import com.elias.attendancecontrol.persistence.repository.AttendanceRepository;
import com.elias.attendancecontrol.persistence.repository.QrTokenRepository;
import com.elias.attendancecontrol.persistence.repository.SessionRepository;
import com.elias.attendancecontrol.persistence.repository.UserRepository;
import com.elias.attendancecontrol.service.AttendanceService;
import com.elias.attendancecontrol.service.EnrollmentService;
import com.elias.attendancecontrol.service.LogService;
import com.elias.attendancecontrol.service.TokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttendanceServiceImpl implements AttendanceService {
    private final AttendanceRepository attendanceRepository;
    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;
    private final QrTokenRepository qrTokenRepository;
    private final TokenService tokenService;
    private final LogService logService;
    private final EnrollmentService enrollmentService;
    @Override
    @Transactional
    public Attendance registerAttendance(Long userId, String qrToken) {
        log.debug("Attempting to register attendance for user: {} with QR token", userId);
        if (!tokenService.validateQR(qrToken)) {
            throw new IllegalArgumentException("Código QR inválido o expirado");
        }
        QRToken qr = qrTokenRepository.findByToken(qrToken)
                .orElseThrow(() -> new IllegalArgumentException("Token QR no encontrado"));
        Session session = qr.getSession();
        validateSessionTolerance(session);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        if (!user.getActive()) {
            throw new IllegalStateException("Usuario inactivo. Contacte al administrador.");
        }
        if (!validateSession(session.getId())) {
            throw new IllegalStateException("La sesión no está activa para registro de asistencia");
        }
        Activity activity = session.getActivity();
        if (activity.getRequiresEnrollment() != null && activity.getRequiresEnrollment()) {
            if (!enrollmentService.isUserEnrolled(activity.getId(), userId)) {
                throw new IllegalStateException(
                    "Usuario no inscrito en esta actividad. Contacte al responsable para inscribirse.");
            }
        }
        if (!validateTime(session.getId())) {
            throw new IllegalStateException("Fuera del horario permitido para registro de asistencia");
        }
        if (checkDuplicate(session.getId(), userId)) {
            throw new IllegalStateException("Ya existe un registro de asistencia para esta sesión");
        }
        Attendance attendance = new Attendance();
        attendance.setUser(user);
        attendance.setSession(session);
        attendance.setRegistrationTime(LocalDateTime.now());
        attendance.setStatus(determineStatus(session));
        attendance.setRegistrationType("QR");
        Attendance savedAttendance = attendanceRepository.save(attendance);
        logService.log(builder -> builder
                .eventType("ATTENDANCE_REGISTERED")
                .description("Asistencia registrada por QR")
                .user(user)
                .session(session)
                .details("Estado: " + savedAttendance.getStatus())
        );
        log.info("Attendance registered successfully: user={}, session={}, status={}",
            userId, session.getId(), savedAttendance.getStatus());
        return savedAttendance;
    }
    @Override
    @Transactional
    public List<Attendance> manualRegistrationBatch(Long sessionId, Map<Long, AttendanceStatus> userAttendances) {
        log.debug("Manual batch attendance registration/update for {} users in session: {}", userAttendances.size(), sessionId);

        if (userAttendances.isEmpty()) {
            throw new IllegalArgumentException("La lista de asistencias no puede estar vacía");
        }

        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Sesión no encontrada"));

        validateSessionTolerance(session);

        if (session.getStatus() == SessionStatus.CLOSED) {
            throw new IllegalStateException("No se puede editar la asistencia, la sesión está cerrada");
        }

        if (session.getStatus() != SessionStatus.ACTIVE) {
            throw new IllegalStateException("La sesión no está activa. Estado: " + session.getStatus());
        }

        List<Attendance> attendances = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        int createdCount = 0;
        int updatedCount = 0;

        for (Map.Entry<Long, AttendanceStatus> entry : userAttendances.entrySet()) {
            Long userId = entry.getKey();
            AttendanceStatus newStatus = entry.getValue();

            try {
                User user = userRepository.findById(userId)
                        .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + userId));

                if (!user.getActive()) {
                    errors.add("Usuario inactivo: " + user.getUsername());
                    continue;
                }

                Optional<Attendance> existingOpt = attendanceRepository.findBySession_IdAndUser_Id(sessionId, userId);

                Attendance attendance;
                if (existingOpt.isPresent()) {
                    attendance = existingOpt.get();
                    AttendanceStatus oldStatus = attendance.getStatus();

                    if (oldStatus != newStatus) {
                        attendance.setStatus(newStatus);
                        attendance.setRegistrationTime(LocalDateTime.now());

                        Attendance savedAttendance = attendanceRepository.save(attendance);
                        attendances.add(savedAttendance);
                        updatedCount++;

                        logService.log(builder -> builder
                                .eventType("ATTENDANCE_UPDATED")
                                .description("Asistencia actualizada manualmente")
                                .user(user)
                                .session(session)
                                .details(String.format("Usuario: %s, Estado anterior: %s, Nuevo estado: %s",
                                        user.getUsername(), oldStatus, newStatus))
                        );

                        log.debug("Attendance updated: user={}, session={}, {} -> {}",
                                userId, sessionId, oldStatus, newStatus);
                    } else {
                        attendances.add(attendance);
                        log.debug("Attendance unchanged: user={}, session={}, status={}",
                                userId, sessionId, newStatus);
                    }
                } else {
                    attendance = new Attendance();
                    attendance.setUser(user);
                    attendance.setSession(session);
                    attendance.setRegistrationTime(LocalDateTime.now());
                    attendance.setStatus(newStatus);
                    attendance.setRegistrationType("MANUAL");

                    Attendance savedAttendance = attendanceRepository.save(attendance);
                    attendances.add(savedAttendance);
                    createdCount++;

                    logService.log(builder -> builder
                            .eventType("ATTENDANCE_MANUAL")
                            .description("Asistencia registrada manualmente")
                            .user(user)
                            .session(session)
                            .details("Usuario: " + user.getUsername() + ", Estado: " + newStatus)
                    );

                    log.debug("Attendance created: user={}, session={}, status={}",
                            userId, sessionId, newStatus);
                }

            } catch (Exception e) {
                log.warn("Error processing attendance for user {}: {}", userId, e.getMessage());
                errors.add("Error con usuario " + userId + ": " + e.getMessage());
            }
        }

        log.info("Manual batch attendance completed: {} created, {} updated, {} errors for session: {}",
                createdCount, updatedCount, errors.size(), sessionId);

        if (!errors.isEmpty()) {
            log.warn("Errors during batch registration: {}", String.join(", ", errors));
        }

        if (attendances.isEmpty()) {
            throw new IllegalStateException("No se pudo procesar ninguna asistencia. Errores: " + String.join(", ", errors));
        }

        return attendances;
    }
    @Override
    @Transactional(readOnly = true)
    public List<Attendance> listAttendance() {
        return attendanceRepository.findAll();
    }
    @Transactional(readOnly = true)
    @Override
    public boolean validateSession(Long sessionId) {
        try {
            Session session = sessionRepository.findById(sessionId)
                    .orElseThrow(() -> new IllegalArgumentException("Sesión no encontrada"));
            return session.getStatus() == SessionStatus.ACTIVE;
        } catch (IllegalArgumentException e) {
            log.warn("Session validation failed: {}", e.getMessage());
            return false;
        }
    }
    @Transactional(readOnly = true)
    @Override
    public boolean validateUser(Long userId) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
            return user.getActive();
        } catch (IllegalArgumentException e) {
            log.warn("User validation failed: {}", e.getMessage());
            return false;
        }
    }
    @Transactional(readOnly = true)
    @Override
    public boolean validateTime(Long sessionId) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Sesión no encontrada"));
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime sessionStart = LocalDateTime.of(session.getSessionDate(), session.getStartTime());
        int tolerance = session.getToleranceMinutes();
        LocalDateTime allowedStart = sessionStart.minusMinutes(tolerance);
        LocalDateTime allowedEnd = sessionStart.plusMinutes((long) tolerance * 2);
        boolean isValid = now.isAfter(allowedStart) && now.isBefore(allowedEnd);
        log.debug("Time validation for session {}: now={}, allowed window=[{} to {}], tolerance={} min, valid={}",
            sessionId, now, allowedStart, allowedEnd, tolerance, isValid);
        return isValid;
    }
    @Transactional(readOnly = true)
    @Override
    public boolean checkDuplicate(Long sessionId, Long userId) {
        boolean exists = attendanceRepository.existsBySession_IdAndUser_Id(sessionId, userId);
        if (exists) {
            log.debug("Duplicate attendance found for user {} in session {}", userId, sessionId);
        }
        return exists;
    }
    @Transactional(readOnly = true)
    @Override
    public List<Attendance> getAttendanceBySession(Long sessionId) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Sesión no encontrada"));
        return attendanceRepository.findBySession(session);
    }
    @Transactional(readOnly = true)
    @Override
    public List<Attendance> getAttendanceByUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        return attendanceRepository.findByUser(user);
    }
    private AttendanceStatus determineStatus(Session session) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime sessionStart = LocalDateTime.of(session.getSessionDate(), session.getStartTime());
        int tolerance = session.getToleranceMinutes();
        if (now.isAfter(sessionStart.plusMinutes(tolerance))) {
            log.debug("Attendance marked as LATE for session {} (arrived {} min after tolerance)",
                session.getId(), tolerance);
            return AttendanceStatus.LATE;
        }
        log.debug("Attendance marked as PRESENT for session {}", session.getId());
        return AttendanceStatus.PRESENT;
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
                String.format("La tolerancia (%d min) excede la duración de la sesión (%d min). " +
                    "No se puede registrar asistencia.",
                    session.getToleranceMinutes(), sessionDuration));
        }
    }
}
