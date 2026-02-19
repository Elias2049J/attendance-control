package com.elias.attendancecontrol.controller;
import com.elias.attendancecontrol.service.ActivityService;
import com.elias.attendancecontrol.service.CalendarService;
import com.elias.attendancecontrol.service.SessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
@Slf4j
@Controller
@RequestMapping("/calendar")
@RequiredArgsConstructor
public class CalendarController {
    private final CalendarService calendarService;
    private final SessionService sessionService;
    private final ActivityService activityService;
    @GetMapping
    public String getCalendarView(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                  Model model) {
        log.debug("Getting calendar view from {} to {}", startDate, endDate);
        if (startDate == null) {
            startDate = LocalDate.now().withDayOfMonth(1);
        }
        if (endDate == null) {
            endDate = startDate.plusMonths(1).minusDays(1);
        }
        model.addAttribute("sessions", calendarService.getCalendarView(startDate, endDate));
        model.addAttribute("activities", activityService.listActivities());
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        return "calendar/view";
    }
    @GetMapping("/activities")
    @ResponseBody
    public Object getActivitiesByDateRange(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                           @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        log.debug("Getting activities from {} to {}", startDate, endDate);
        return calendarService.getActivitiesByDateRange(startDate, endDate);
    }
}
