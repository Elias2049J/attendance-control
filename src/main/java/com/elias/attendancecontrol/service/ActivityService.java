package com.elias.attendancecontrol.service;
import com.elias.attendancecontrol.model.entity.Activity;
import com.elias.attendancecontrol.model.entity.ActivityStatus;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
public interface ActivityService {
    Activity createActivity(Activity activity);
    Activity updateActivity(Long id, Activity activity);
    void activateActivity(Long id);
    List<Activity> listActivities();
    Activity getActivityById(Long id);
    @Transactional(readOnly = true)
    List<Activity> findActiveActivities();
    @Transactional(readOnly = true)
    List<Activity> findByResponsible(Long userId);
    void pauseActivity(Long activityId);
    void completeActivity(Long activityId);
    void cancelActivity(Long activityId);
    boolean canPublish(Long activityId);
    boolean canComplete(Long activityId);
    void changeStatus(Long activityId, ActivityStatus newStatus);
    List<Activity> searchActivities(String query, Long userId, String role);
}
