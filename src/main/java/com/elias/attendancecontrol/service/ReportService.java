package com.elias.attendancecontrol.service;
import java.time.LocalDate;
import java.util.Map;
public interface ReportService {
    Map<String, Object> generateReport(LocalDate startDate, LocalDate endDate);
    Map<String, Object> generateActivityReport(Long activityId, LocalDate startDate, LocalDate endDate);
    Map<String, Object> generateUserReport(Long userId, LocalDate startDate, LocalDate endDate);
    byte[] exportReport(Map<String, Object> reportData, String format);
    double calculateAttendanceRate(Long activityId, LocalDate startDate, LocalDate endDate);
    Map<String, Object> calculateStatistics(Long activityId);
    Map<String, Object> aggregateData(Long activityId, LocalDate startDate, LocalDate endDate);
    Map<String, Object> generateEnrollmentAttendanceReport(Long activityId);
    Map<String, Object> getParticipantStatistics(Long activityId, Long userId);
}
