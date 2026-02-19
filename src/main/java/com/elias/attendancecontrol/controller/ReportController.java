package com.elias.attendancecontrol.controller;
import com.elias.attendancecontrol.service.ActivityService;
import com.elias.attendancecontrol.service.ReportService;
import com.elias.attendancecontrol.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
@Slf4j
@Controller
@RequestMapping("/reports")
@RequiredArgsConstructor
public class ReportController {
    private final ReportService reportService;
    private final ActivityService activityService;
    private final UserService userService;
    @GetMapping
    public String showReportsMenu(Model model) {
        log.debug("Showing reports menu");
        model.addAttribute("activities", activityService.listActivities());
        model.addAttribute("users", userService.listUsers());
        return "reports/menu";
    }
    @PostMapping("/generate")
    public String generateReport(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                 @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                 Model model) {
        log.debug("Generating general report from {} to {}", startDate, endDate);
        try {
            var reportData = reportService.generateReport(startDate, endDate);
            model.addAttribute("reportData", reportData);
            model.addAttribute("startDate", startDate);
            model.addAttribute("endDate", endDate);
            return "reports/general";
        } catch (Exception e) {
            log.error("Error generating report", e);
            model.addAttribute("error", "Error al generar el reporte: " + e.getMessage());
            return "reports/menu";
        }
    }
    @PostMapping("/activity/{activityId}")
    public String generateActivityReport(@PathVariable Long activityId,
                                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                         Model model) {
        log.debug("Generating activity report for activity: {} from {} to {}", activityId, startDate, endDate);
        try {
            var reportData = reportService.generateActivityReport(activityId, startDate, endDate);
            model.addAttribute("reportData", reportData);
            model.addAttribute("activityId", activityId);
            model.addAttribute("startDate", startDate);
            model.addAttribute("endDate", endDate);
            return "reports/activity";
        } catch (Exception e) {
            log.error("Error generating activity report", e);
            model.addAttribute("error", "Error al generar el reporte: " + e.getMessage());
            return "reports/menu";
        }
    }
    @PostMapping("/user/{userId}")
    public String generateUserReport(@PathVariable Long userId,
                                     @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                     @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                     Model model) {
        log.debug("Generating user report for user: {} from {} to {}", userId, startDate, endDate);
        try {
            var reportData = reportService.generateUserReport(userId, startDate, endDate);
            model.addAttribute("reportData", reportData);
            model.addAttribute("userId", userId);
            model.addAttribute("startDate", startDate);
            model.addAttribute("endDate", endDate);
            return "reports/user";
        } catch (Exception e) {
            log.error("Error generating user report", e);
            model.addAttribute("error", "Error al generar el reporte: " + e.getMessage());
            return "reports/menu";
        }
    }
}
