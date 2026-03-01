package com.elias.attendancecontrol.service.implementation;
import com.elias.attendancecontrol.model.dto.ActivityUserReportDTO;
import com.elias.attendancecontrol.model.dto.ActivityAttendanceSummaryDTO;
import com.elias.attendancecontrol.model.dto.UserActivityReportDTO;
import com.elias.attendancecontrol.model.entity.*;
import com.elias.attendancecontrol.persistence.repository.*;
import com.elias.attendancecontrol.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {
    private final SessionRepository sessionRepository;
    private final AttendanceRepository attendanceRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final ActivityService activityService;
    private final UserService userService;

    @Override
    public Map<String, Object> generateActivityReport(Long activityId, LocalDate startDate, LocalDate endDate) {
        List<ActivityUserReportDTO> activityUserReportDTOList = attendanceRepository.getActivityUserReport(activityId, startDate, endDate);
        return Map.of(
                "timestamp", LocalDateTime.now(),
                "details", activityUserReportDTOList,
                "rows", activityUserReportDTOList.size(),
                "startDate", startDate,
                "endDate", endDate
        );
    }

    @Override
    public Map<String, Object> generateUserReport(Long userId, LocalDate startDate, LocalDate endDate) {
        List<UserActivityReportDTO> activityUserReportDTOList = attendanceRepository.getUserActivityReport(userId, startDate, endDate);
        return Map.of(
                "timestamp", LocalDateTime.now(),
                "details", activityUserReportDTOList,
                "rows", activityUserReportDTOList.size(),
                "startDate", startDate,
                "endDate", endDate
        );
    }

    @Override
    public Map<String, Object> generateActivitiesUsersGeneralReport(LocalDate startDate, LocalDate endDate) {
        List<ActivityAttendanceSummaryDTO> summaryList = attendanceRepository.getGeneralActivityReport(startDate, endDate);
        return Map.of(
                "details", summaryList,
                "rows", summaryList.size(),
                "timestamp", LocalDateTime.now()
        );
    }

    @Override
    public Map<String, Object> generateLogsReport(LocalDate startDate, LocalDate endDate) {
        return Map.of();
    }

    @Override
    public double calculateAttendanceRate(Long activityId, LocalDate startDate, LocalDate endDate) {
        return 0;
    }

    @Override
    public Map<String, Object> calculateStatistics(Long activityId) {
        return Map.of();
    }

    @Override
    public Map<String, Object> aggregateData(Long activityId, LocalDate startDate, LocalDate endDate) {
        return Map.of();
    }
    @Override
    public Map<String, Object> generateEnrollmentAttendanceReport(Long activityId) {
        log.debug("Generating enrollment attendance report for activity: {}", activityId);
        Activity activity = activityService.getActivityById(activityId);
        List<User> enrolledUsers = enrollmentRepository.findUsersByActivityAndStatus(
                activity, EnrollmentStatus.ENROLLED);
        List<Session> sessions = sessionRepository.findByActivityOrderBySessionDateAsc(activity);
        Map<String, Object> report = new HashMap<>();
        report.put("activity", activity);
        report.put("totalEnrolled", enrolledUsers.size());
        report.put("totalSessions", sessions.size());
        List<Map<String, Object>> participantStats = enrolledUsers.stream()
                .map(user -> {
                    long totalAttended = attendanceRepository.findByUser(user).stream()
                            .filter(att -> sessions.contains(att.getSession()))
                            .count();
                    double attendanceRate = sessions.isEmpty() ? 0 :
                            (totalAttended * 100.0 / sessions.size());
                    Map<String, Object> stats = new HashMap<>();
                    stats.put("user", user);
                    stats.put("totalAttended", totalAttended);
                    stats.put("totalAbsent", sessions.size() - totalAttended);
                    stats.put("attendanceRate", attendanceRate);
                    return stats;
                })
                .toList();
        report.put("participants", participantStats);
        log.info("Enrollment attendance report generated for activity: {}", activityId);
        return report;
    }

    @Override
    public Map<String, Object> getParticipantStatistics(Long activityId, Long userId) {
        log.debug("Getting participant statistics: activity={}, user={}", activityId, userId);
        Activity activity = activityService.getActivityById(activityId);
        boolean isEnrolled = enrollmentRepository.existsByActivityAndUserAndStatus(
                activity,
                userService.getUserById(userId),
                EnrollmentStatus.ENROLLED
        );
        if (!isEnrolled) {
            throw new IllegalArgumentException("El usuario no está inscrito en esta actividad");
        }
        List<Session> sessions = sessionRepository.findByActivityOrderBySessionDateAsc(activity);
        List<Attendance> attendances = attendanceRepository.findByUser(
                userService.getUserById(userId)
        ).stream()
                .filter(att -> sessions.contains(att.getSession()))
                .toList();
        Map<String, Object> stats = new HashMap<>();
        stats.put("activity", activity);
        stats.put("totalSessions", sessions.size());
        stats.put("totalAttended", attendances.size());
        stats.put("totalAbsent", sessions.size() - attendances.size());
        stats.put("attendanceRate", sessions.isEmpty() ? 0 :
                (attendances.size() * 100.0 / sessions.size()));
        long present = attendances.stream()
                .filter(att -> att.getStatus().isPresent())
                .count();
        long late = attendances.stream()
                .filter(att -> att.getStatus().isLate())
                .count();
        stats.put("present", present);
        stats.put("late", late);
        return stats;
    }
}
