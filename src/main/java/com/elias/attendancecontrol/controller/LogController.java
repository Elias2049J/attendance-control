package com.elias.attendancecontrol.controller;
import com.elias.attendancecontrol.config.SecurityUtils;
import com.elias.attendancecontrol.service.LogService;
import com.elias.attendancecontrol.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
@Slf4j
@Controller
@RequestMapping("/audit")
@RequiredArgsConstructor
public class LogController {
    private final LogService logService;
    private final UserService userService;
    private final SecurityUtils securityUtils;
    @GetMapping
    public String listAuditLogs(Model model) {
        log.debug("Listing audit logs");
        model.addAttribute("auditLogs", logService.listAuditLogs());
        model.addAttribute("users", userService.listUsers());
        model.addAttribute("isSystemAdmin", securityUtils.isSystemAdmin());
        return "audit/list";
    }
    @GetMapping("/search/results")
    public String searchLogsFragment(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            Model model) {
        log.debug("Search logs fragment: userId={}, eventType={}, startDate={}, endDate={}",
                userId, eventType, startDate, endDate);
        model.addAttribute("auditLogs", logService.searchLogs(userId, eventType, startDate, endDate));
        model.addAttribute("selectedUserId", userId);
        model.addAttribute("selectedEventType", eventType);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        return "fragments/audit-results :: resultsTable";
    }
    @GetMapping("/filter")
    public String filterAuditLogs(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            Model model) {
        log.debug("Filtering audit logs - userId: {}, eventType: {}, startDate: {}, endDate: {}",
            userId, eventType, startDate, endDate);
        try {
            model.addAttribute("auditLogs", logService.searchLogs(userId, eventType, startDate, endDate));
            model.addAttribute("users", userService.listUsers());
            model.addAttribute("selectedUserId", userId);
            model.addAttribute("selectedEventType", eventType);
            model.addAttribute("startDate", startDate);
            model.addAttribute("endDate", endDate);
            model.addAttribute("isSystemAdmin", securityUtils.isSystemAdmin());
        } catch (Exception e) {
            log.error("Error filtering audit logs", e);
            model.addAttribute("error", "Error al filtrar logs: " + e.getMessage());
            model.addAttribute("auditLogs", logService.listAuditLogs());
            model.addAttribute("users", userService.listUsers());
            model.addAttribute("isSystemAdmin", securityUtils.isSystemAdmin());
        }
        return "audit/list";
    }
}
