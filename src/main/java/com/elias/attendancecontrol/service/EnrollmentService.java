package com.elias.attendancecontrol.service;
import com.elias.attendancecontrol.model.entity.Activity;
import com.elias.attendancecontrol.model.entity.Enrollment;
import com.elias.attendancecontrol.model.entity.User;
import java.util.List;
public interface EnrollmentService {
    Enrollment enrollUser(Long activityId, Long userId);
    void enrollMultipleUsers(Long activityId, List<Long> userIds);
    void removeParticipant(Long activityId, Long userId);
    List<User> getEnrolledParticipants(Long activityId);
    List<Activity> getActivitiesByUser(Long userId);
    boolean isUserEnrolled(Long activityId, Long userId);
    List<User> getExpectedParticipants(Long sessionId);
    List<User> getAbsentUsers(Long sessionId);
    List<Enrollment> listEnrollments();
    Enrollment getEnrollmentById(Long id);
    List<Enrollment> getEnrollmentsByActivity(Long activityId);
    long getEnrolledCount(Long activityId);
    void completeAllEnrollments(Long activityId);
}
