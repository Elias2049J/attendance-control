package com.elias.attendancecontrol.service;
import com.elias.attendancecontrol.model.entity.ActivityIncident;
import java.time.LocalDate;
import java.util.List;
public interface ActivityIncidentService {
    ActivityIncident registerIncident(ActivityIncident exception);
    ActivityIncident rescheduleOccurrence(Long activityId, LocalDate originalDate, LocalDate newDate, String reason);
    ActivityIncident cancelOccurrence(Long activityId, LocalDate date, String reason);
    List<ActivityIncident> getAllIncidents();
    List<ActivityIncident> getIncidentsByActivity(Long activityId);
    ActivityIncident getIncidentById(Long id);
}
