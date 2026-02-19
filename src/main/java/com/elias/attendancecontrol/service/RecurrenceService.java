package com.elias.attendancecontrol.service;
import com.elias.attendancecontrol.model.entity.RecurrenceRule;
public interface RecurrenceService {
    RecurrenceRule createRecurrenceRule(Long activityId, RecurrenceRule recurrenceRule);
    RecurrenceRule updateRecurrenceRule(Long ruleId, RecurrenceRule recurrenceRule);
    RecurrenceRule configureRecurrence(Long activityId, RecurrenceRule recurrenceRule);
    RecurrenceRule updateRecurrence(Long id, RecurrenceRule recurrenceRule);
    boolean validateRecurrence(RecurrenceRule recurrenceRule);
    void generateSessionsForActivity(Long activityId);
}
