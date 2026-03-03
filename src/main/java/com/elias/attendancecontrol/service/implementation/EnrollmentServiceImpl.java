package com.elias.attendancecontrol.service.implementation;
import com.elias.attendancecontrol.config.SecurityUtils;
import com.elias.attendancecontrol.model.entity.*;
import com.elias.attendancecontrol.persistence.repository.*;
import com.elias.attendancecontrol.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
@Slf4j
@Service
@RequiredArgsConstructor
public class EnrollmentServiceImpl implements EnrollmentService {
    private final EnrollmentRepository enrollmentRepository;
    private final AttendanceRepository attendanceRepository;
    private final LogService logService;
    private final SecurityUtils securityUtils;
    private final ActivityRepository activityRepository;
    private final UserService userService;
    private final SessionService sessionService;

    @Override
    @Transactional
    public Enrollment enrollUser(Long activityId, Long userId) {
        log.debug("Enrolling user {} in activity {}", userId, activityId);
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new IllegalArgumentException("Actividad no encontrada"));
        User user = userService.getUserById(userId);
        if (!activity.getStatus().isScheduled()) {
            throw new IllegalStateException("No se puede inscribir en una actividad inactiva");
        }
        if (!user.getActive()) {
            throw new IllegalStateException("No se puede inscribir un usuario inactivo");
        }
        if (enrollmentRepository.existsByActivityAndUserAndStatus(activity, user, EnrollmentStatus.ENROLLED)) {
            throw new IllegalStateException("El usuario ya está inscrito en esta actividad");
        }
        Enrollment enrollment = new Enrollment();
        enrollment.setActivity(activity);
        enrollment.setUser(user);
        enrollment.setEnrollmentDate(LocalDateTime.now());
        enrollment.setStatus(EnrollmentStatus.ENROLLED);
        enrollment.setEnrolledByUser(securityUtils.getCurrentUser().orElse(null));
        Enrollment savedEnrollment = enrollmentRepository.save(enrollment);
        logService.log(builder -> builder
                .eventType("USER_ENROLLED")
                .description("Usuario inscrito en actividad: " + activity.getName())
                .user(user)
                .organization(user.getOrganization())
                .details("Activity ID: " + activityId + ", Enrolled by: " +
                        (enrollment.getEnrolledByUser() != null ? enrollment.getEnrolledByUser().getUsername() : "System"))
        );
        log.info("User {} enrolled in activity {} successfully", userId, activityId);
        return savedEnrollment;
    }
    @Override
    @Transactional
    public void enrollMultipleUsers(Long activityId, List<Long> userIds) {
        log.debug("Enrolling {} users in activity {}", userIds.size(), activityId);
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new IllegalArgumentException("Actividad no encontrada"));
        if (!activity.getStatus().isScheduled()) {
            throw new IllegalStateException("No se puede inscribir en una actividad inactiva");
        }
        User enrolledBy = securityUtils.getCurrentUser().orElse(null);
        int successCount = 0;
        int skipCount = 0;
        for (Long userId : userIds) {
            try {
                User user = userService.getUserById(userId);
                if (!user.getActive()) {
                    log.warn("Skipping inactive user: {}", userId);
                    skipCount++;
                    continue;
                }
                if (enrollmentRepository.existsByActivityAndUserAndStatus(activity, user, EnrollmentStatus.ENROLLED)) {
                    log.debug("User {} already enrolled in activity {}", userId, activityId);
                    skipCount++;
                    continue;
                }
                Enrollment enrollment = new Enrollment();
                enrollment.setActivity(activity);
                enrollment.setUser(user);
                enrollment.setEnrollmentDate(LocalDateTime.now());
                enrollment.setStatus(EnrollmentStatus.ENROLLED);
                enrollment.setEnrolledByUser(enrolledBy);
                enrollmentRepository.save(enrollment);
                successCount++;
            } catch (Exception e) {
                log.error("Error enrolling user {}: {}", userId, e.getMessage());
                skipCount++;
            }
        }
        int finalSuccessCount = successCount;
        int finalSkipCount = skipCount;
        logService.log(builder -> builder
            .eventType("BULK_ENROLLMENT")
            .description("Inscripción masiva en actividad: " + activity.getName())
            .user(enrolledBy)
                .organization(securityUtils.getCurrentOrganization().orElse(null))
            .details("Inscritos: " + finalSuccessCount + ", Omitidos: " + finalSkipCount + ", Total intentos: " + userIds.size())
        );
        log.info("Bulk enrollment completed: {} success, {} skipped", successCount, skipCount);
    }

    @Override
    @Transactional
    public void removeParticipant(Long activityId, Long userId) {
        log.debug("Removing user {} from activity {}", userId, activityId);
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new IllegalArgumentException("Actividad no encontrada"));
        User user = userService.getUserById(userId);
        Enrollment enrollment = enrollmentRepository.findByActivityAndUser(activity, user)
                .orElseThrow(() -> new IllegalArgumentException("Inscripción no encontrada"));
        enrollment.setStatus(EnrollmentStatus.DROPPED);
        enrollmentRepository.save(enrollment);
        logService.log(builder -> builder
                .eventType("USER_DROPPED")
                .description("Usuario dado de baja de actividad: " + activity.getName())
                .user(user)
                .organization(user.getOrganization())
                .details("Activity ID: " + activityId + ", Removed by: " +
                        securityUtils.getCurrentUser().map(User::getUsername).orElse("System"))
        );
        log.info("User {} removed from activity {} successfully", userId, activityId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> getEnrolledParticipants(Long activityId) {
        log.debug("Getting enrolled participants for activity {}", activityId);
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new IllegalArgumentException("Actividad no encontrada"));
        return enrollmentRepository.findUsersByActivityAndStatus(activity, EnrollmentStatus.ENROLLED);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Activity> getActivitiesByUser(Long userId) {
        log.debug("Getting activities for user {}", userId);
        User user = userService.getUserById(userId);
        return enrollmentRepository.findActivitiesByUserAndStatusWithDetails(user, EnrollmentStatus.ENROLLED);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isUserEnrolled(Long activityId, Long userId) {
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new IllegalArgumentException("Actividad no encontrada"));
        User user = userService.getUserById(userId);
        return enrollmentRepository.existsByActivityAndUserAndStatus(activity, user, EnrollmentStatus.ENROLLED);
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> getExpectedParticipants(Long sessionId) {
        log.debug("Getting expected participants for session {}", sessionId);
        Session session = sessionService.getSessionById(sessionId);
        Activity activity = session.getActivity();
        return enrollmentRepository.findUsersByActivityAndStatus(activity, EnrollmentStatus.ENROLLED);
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> getAbsentUsers(Long sessionId) {
        log.debug("Getting absent users for session {}", sessionId);
        Session session = sessionService.getSessionById(sessionId);
        Activity activity = session.getActivity();
        List<User> enrolledUsers = enrollmentRepository.findUsersByActivityAndStatus(
                activity, EnrollmentStatus.ENROLLED);
        List<User> attendedUsers = attendanceRepository.findBySession(session)
                .stream()
                .map(Attendance::getUser)
                .toList();
        return enrolledUsers.stream()
                .filter(user -> !attendedUsers.contains(user))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Enrollment> listEnrollments() {
        return enrollmentRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Enrollment getEnrollmentById(Long id) {
        return enrollmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Inscripción no encontrada"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Enrollment> getEnrollmentsByActivity(Long activityId) {
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new IllegalArgumentException("Actividad no encontrada"));
        return enrollmentRepository.findByActivity(activity);
    }

    @Override
    @Transactional(readOnly = true)
    public long getEnrolledCount(Long activityId) {
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new IllegalArgumentException("Actividad no encontrada"));
        return enrollmentRepository.countByActivityAndStatus(activity, EnrollmentStatus.ENROLLED);
    }

    @Override
    @Transactional
    public void completeAllEnrollments(Long activityId) {
        log.debug("Completing all enrollments for activity: {}", activityId);
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new IllegalArgumentException("Actividad no encontrada"));
        List<Enrollment> activeEnrollments = enrollmentRepository.findByActivityAndStatus(activity, EnrollmentStatus.ENROLLED);
        for (Enrollment enrollment : activeEnrollments) {
            enrollment.setStatus(EnrollmentStatus.COMPLETED);
            enrollmentRepository.save(enrollment);
        }
        log.info("Completed {} enrollments for activity: {}", activeEnrollments.size(), activityId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Enrollment> getEnrollmentsWithoutAttendance(Long activityId, List<Attendance> attendances) {
        log.debug("Getting enrollments without attendance for activity: {}", activityId);

        List<Enrollment> enrollments = getEnrollmentsByActivity(activityId);

        Set<Long> attendedUserIds = attendances.stream()
                .map(att -> att.getUser().getId())
                .collect(Collectors.toSet());


        List<Enrollment> enrollmentsWithoutAttendance = enrollments.stream()
                .filter(enrollment -> !attendedUserIds.contains(enrollment.getUser().getId()))
                .collect(Collectors.toList());

        log.debug("Found {} enrollments without attendance", enrollmentsWithoutAttendance.size());
        return enrollmentsWithoutAttendance;
    }
}
