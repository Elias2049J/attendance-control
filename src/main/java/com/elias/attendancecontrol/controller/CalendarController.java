package com.elias.attendancecontrol.controller;
import com.elias.attendancecontrol.model.dto.CalendarEventDTO;
import com.elias.attendancecontrol.service.ActivityService;
import com.elias.attendancecontrol.service.CalendarService;
import com.elias.attendancecontrol.service.SessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Controller
@RequestMapping("/calendar")
@RequiredArgsConstructor
public class CalendarController {
    private final CalendarService calendarService;
    private final SessionService sessionService;
    private final ActivityService activityService;

    @GetMapping
    public String getCalendarView(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
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
        model.addAttribute("activities", activityService.listActivitiesSorted());
        String eventsJson = "[]";
        try {
            eventsJson = new ObjectMapper().writeValueAsString(
                    calendarService.getCalendarEventsForJson(startDate, endDate));
        } catch (Exception e) {
            log.error("Error serializing calendar events to JSON", e);
        }
        model.addAttribute("eventsJson", eventsJson);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("activeMenu", "calendar");
        return "calendar/view";
    }

    @GetMapping("/activities")
    @ResponseBody
    public Object getActivitiesByDateRange(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                           @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        log.debug("Getting activities from {} to {}", startDate, endDate);
        return calendarService.getActivitiesByDateRange(startDate, endDate);
    }

    @GetMapping("/events")
    @ResponseBody
    public List<CalendarEventDTO> getEventsJson(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return calendarService.getCalendarEventsForJson(startDate, endDate);
    }
}
