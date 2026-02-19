package com.elias.attendancecontrol.service;
import com.elias.attendancecontrol.model.entity.Activity;
import com.elias.attendancecontrol.model.entity.Session;
import java.time.LocalDate;
import java.util.List;
public interface CalendarService {
    List<Session> getCalendarView(LocalDate startDate, LocalDate endDate);
    List<Activity> getActivitiesByDateRange(LocalDate startDate, LocalDate endDate);
}
