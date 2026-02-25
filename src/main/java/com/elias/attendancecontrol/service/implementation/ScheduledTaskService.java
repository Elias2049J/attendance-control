package com.elias.attendancecontrol.service.implementation;
import com.elias.attendancecontrol.model.entity.Activity;
import com.elias.attendancecontrol.model.entity.ActivityStatus;
import com.elias.attendancecontrol.persistence.repository.ActivityRepository;
import com.elias.attendancecontrol.service.ActivityService;
import com.elias.attendancecontrol.service.TokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.util.List;
@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduledTaskService {
    private final ActivityRepository activityRepository;
    private final ActivityService activityService;
    private final TokenService tokenService;

    @Scheduled(cron = "${scheduler.activity-completion.cron}")
    public void autoCompleteFinishedActivities() {
        log.info("Starting auto-completion check for finished activities");
        List<Activity> scheduledActivities = activityRepository.findByStatusOrderByIdDesc(ActivityStatus.SCHEDULED);
        int completedCount = 0;
        for (Activity activity : scheduledActivities) {
            try {
                if (activityService.canComplete(activity.getId())) {
                    activityService.completeActivity(activity.getId());
                    completedCount++;
                    log.info("Auto-completed activity: {} (ID: {})", activity.getName(), activity.getId());
                }
            } catch (Exception e) {
                log.error("Error auto-completing activity {}: {}", activity.getId(), e.getMessage());
            }
        }
        log.info("Auto-completion check completed: {} activities completed", completedCount);
    }

    @Scheduled(fixedRateString = "${scheduler.qr-regeneration.fixed-rate.ms}")
    public void autoRegenerateActiveSessionQRs() {
        log.debug("Starting auto-regeneration of QR codes for active sessions");
        try {
            tokenService.autoRegenerateActiveSessionQRs();
        } catch (Exception e) {
            log.error("Error in auto-regeneration task: {}", e.getMessage());
        }
    }
}
