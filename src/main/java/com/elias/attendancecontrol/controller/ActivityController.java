package com.elias.attendancecontrol.controller;
import com.elias.attendancecontrol.config.SecurityUtils;
import com.elias.attendancecontrol.model.entity.*;
import com.elias.attendancecontrol.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
@Slf4j
@Controller
@RequestMapping("/activities")
@RequiredArgsConstructor
public class ActivityController {
    private final ActivityService activityService;
    private final UserService userService;
    private final SessionService sessionService;
    private final AttendanceService attendanceService;
    private final EnrollmentService enrollmentService;
    private final RecurrenceService recurrenceService;
    private final SecurityUtils securityUtils;

    @GetMapping
    public String listActivities(Model model) {
        log.debug("Listing all activities");
        List<Activity> activities = activityService.listActivities();
        model.addAttribute("activities", activities);
        model.addAttribute("activeMenu", "activities");
        return "activities/list";
    }
    @GetMapping("/new")
    public String showCreateForm(Model model) {
        log.debug("Showing activity creation form");
        if (!securityUtils.canManageActivities()) {
            log.warn("User {} attempted to create activity without permission",
                    securityUtils.getCurrentUser().map(User::getUsername).orElse("unknown"));
            return "redirect:/activities?error=no_permission";
        }
        model.addAttribute("activity", new Activity());
        model.addAttribute("users", userService.listUsers());
        model.addAttribute("recurrenceTypes", RecurrenceType.values());
        model.addAttribute("daysOfWeek", DayOfWeek.values());
        model.addAttribute("activeMenu", "activities");
        return "activities/form";
    }
    @PostMapping
    public String createActivity(@Valid @ModelAttribute Activity activity,
                                 @RequestParam(name = "responsibleId", required = false) Long responsibleId,
                                 @RequestParam(name = "recurrenceType") String recurrenceTypeStr,
                                 @RequestParam(name = "startDate", required = false) LocalDate startDate,
                                 @RequestParam(name = "endDate", required = false) LocalDate endDate,
                                 @RequestParam(name = "daysOfWeek", required = false) String daysOfWeek,
                                 @RequestParam(name = "startTime") LocalTime startTime,
                                 @RequestParam(name = "endTime") LocalTime endTime,
                                 @RequestParam(name = "toleranceMinutes", required = false, defaultValue = "0") Integer toleranceMinutes,
                                 BindingResult result,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {
        log.debug("Creating new activity: {}", activity.getName());
        if (!securityUtils.canManageActivities()) {
            return "redirect:/activities?error=no_permission";
        }
        if (responsibleId == null) {
            model.addAttribute("error", "Debe seleccionar un responsable para la actividad");
            model.addAttribute("users", userService.listUsers());
            model.addAttribute("recurrenceTypes", RecurrenceType.values());
            model.addAttribute("daysOfWeek", DayOfWeek.values());
            model.addAttribute("activeMenu", "activities");
            return "activities/form";
        }
        if (result.hasErrors()) {
            model.addAttribute("users", userService.listUsers());
            model.addAttribute("recurrenceTypes", RecurrenceType.values());
            model.addAttribute("daysOfWeek", DayOfWeek.values());
            model.addAttribute("activeMenu", "activities");
            return "activities/form";
        }
        try {
            User responsible = userService.getUserById(responsibleId);
            activity.setResponsible(responsible);

            Activity savedActivity = activityService.createActivity(activity);

            RecurrenceType recurrenceType = RecurrenceType.valueOf(recurrenceTypeStr);
            RecurrenceRule recurrenceRule = new RecurrenceRule();
            recurrenceRule.setRecurrenceType(recurrenceType);
            recurrenceRule.setStartDate(startDate);
            recurrenceRule.setEndDate(endDate);
            recurrenceRule.setDaysOfWeek(daysOfWeek);
            recurrenceRule.setStartTime(startTime);
            recurrenceRule.setEndTime(endTime);
            recurrenceRule.setToleranceMinutes(toleranceMinutes);
            recurrenceRule.setActivity(savedActivity);

            recurrenceService.configureRecurrence(savedActivity.getId(), recurrenceRule);

            redirectAttributes.addFlashAttribute("success", "Actividad creada exitosamente");
            return "redirect:/activities";
        } catch (IllegalArgumentException | IllegalStateException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("users", userService.listUsers());
            model.addAttribute("recurrenceTypes", RecurrenceType.values());
            model.addAttribute("daysOfWeek", DayOfWeek.values());
            model.addAttribute("activeMenu", "activities");
            return "activities/form";
        }
    }
    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        log.debug("Showing edit form for activity: {}", id);
        if (!securityUtils.canManageActivities()) {
            redirectAttributes.addFlashAttribute("error", "No tiene permisos para editar actividades");
            return "redirect:/activities";
        }
        try {
            Activity activity = activityService.getActivityById(id);
            if (activity.getOrganization() != null) {
                securityUtils.validateResourceOwnership(activity.getOrganization().getId());
            }
            model.addAttribute("activity", activity);
            model.addAttribute("users", userService.listUsers());
            model.addAttribute("recurrenceTypes", RecurrenceType.values());
            model.addAttribute("daysOfWeek", DayOfWeek.values());
            model.addAttribute("isEdit", true);
            return "activities/form";
        } catch (SecurityException e) {
            log.warn("User attempted to access activity from different organization: {}", id);
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/activities";
        } catch (IllegalArgumentException e) {
            log.error("Activity not found: {}", id);
            redirectAttributes.addFlashAttribute("error", "Actividad no encontrada");
            return "redirect:/activities";
        }
    }
    @PostMapping("/{id}")
    public String updateActivity(@PathVariable Long id,
                                 @Valid @ModelAttribute Activity activity,
                                 @RequestParam(name = "responsibleId", required = false) Long responsibleId,
                                 @RequestParam(name = "recurrenceType") String recurrenceTypeStr,
                                 @RequestParam(name = "startDate", required = false) LocalDate startDate,
                                 @RequestParam(name = "endDate", required = false) LocalDate endDate,
                                 @RequestParam(name = "daysOfWeek", required = false) String daysOfWeek,
                                 @RequestParam(name = "startTime") LocalTime startTime,
                                 @RequestParam(name = "endTime") LocalTime endTime,
                                 @RequestParam(name = "toleranceMinutes", required = false, defaultValue = "0") Integer toleranceMinutes,
                                 BindingResult result,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {
        log.debug("Updating activity: {}", id);
        if (!securityUtils.canManageActivities()) {
            return "redirect:/activities?error=no_permission";
        }
        if (result.hasErrors()) {
            model.addAttribute("users", userService.listUsers());
            model.addAttribute("recurrenceTypes", RecurrenceType.values());
            model.addAttribute("daysOfWeek", DayOfWeek.values());
            model.addAttribute("isEdit", true);
            return "activities/form";
        }
        try {
            Activity existingActivity = activityService.getActivityById(id);
            if (existingActivity.getOrganization() != null) {
                securityUtils.validateResourceOwnership(existingActivity.getOrganization().getId());
            }
            if (responsibleId != null) {
                User responsible = userService.getUserById(responsibleId);
                activity.setResponsible(responsible);
            }

            activityService.updateActivity(id, activity);

            RecurrenceType recurrenceType = RecurrenceType.valueOf(recurrenceTypeStr);
            RecurrenceRule recurrenceRule;
            if (existingActivity.getRecurrenceRule() != null) {
                recurrenceRule = existingActivity.getRecurrenceRule();
            } else {
                recurrenceRule = new RecurrenceRule();
                recurrenceRule.setActivity(existingActivity);
            }
            recurrenceRule.setRecurrenceType(recurrenceType);
            recurrenceRule.setStartDate(startDate);
            recurrenceRule.setEndDate(endDate);
            recurrenceRule.setDaysOfWeek(daysOfWeek);
            recurrenceRule.setStartTime(startTime);
            recurrenceRule.setEndTime(endTime);
            recurrenceRule.setToleranceMinutes(toleranceMinutes);

            if (existingActivity.getRecurrenceRule() != null) {
                recurrenceService.updateRecurrence(existingActivity.getRecurrenceRule().getId(), recurrenceRule);
            } else {
                recurrenceService.configureRecurrence(id, recurrenceRule);
            }

            redirectAttributes.addFlashAttribute("success", "Actividad actualizada exitosamente");
            return "redirect:/activities";
        } catch (SecurityException e) {
            log.warn("User attempted to update activity from different organization: {}", id);
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/activities";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("users", userService.listUsers());
            model.addAttribute("recurrenceTypes", RecurrenceType.values());
            model.addAttribute("daysOfWeek", DayOfWeek.values());
            model.addAttribute("isEdit", true);
            return "activities/form";
        }
    }

    @PostMapping("/{id}/activate")
    public String activateActivity(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        log.debug("Activating activity: {}", id);
        try {
            Activity activity = activityService.getActivityById(id);
            if (activity.getOrganization() != null) {
                securityUtils.validateResourceOwnership(activity.getOrganization().getId());
            }
            activityService.activateActivity(id);
            redirectAttributes.addFlashAttribute("success", "Actividad activada exitosamente. Las sesiones han sido generadas.");
        } catch (SecurityException e) {
            log.warn("User attempted to activate activity from different organization: {}", id);
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/activities/" + id;
    }

    @PostMapping("/{id}/pause")
    public String pauseActivity(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        log.debug("Pausing activity: {}", id);
        try {
            Activity activity = activityService.getActivityById(id);
            if (activity.getOrganization() != null) {
                securityUtils.validateResourceOwnership(activity.getOrganization().getId());
            }
            activityService.pauseActivity(id);
            redirectAttributes.addFlashAttribute("success", "Actividad pausada exitosamente");
        } catch (SecurityException e) {
            log.warn("User attempted to pause activity from different organization: {}", id);
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (IllegalStateException | IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/activities";
    }
    @PostMapping("/{id}/complete")
    public String completeActivity(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        log.debug("Completing activity: {}", id);
        try {
            Activity activity = activityService.getActivityById(id);
            if (activity.getOrganization() != null) {
                securityUtils.validateResourceOwnership(activity.getOrganization().getId());
            }
            activityService.completeActivity(id);
            redirectAttributes.addFlashAttribute("success", "Actividad completada exitosamente");
        } catch (SecurityException e) {
            log.warn("User attempted to complete activity from different organization: {}", id);
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (IllegalStateException | IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/activities";
    }
    @PostMapping("/{id}/cancel")
    public String cancelActivity(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        log.debug("Cancelling activity: {}", id);
        try {
            Activity activity = activityService.getActivityById(id);
            if (activity.getOrganization() != null) {
                securityUtils.validateResourceOwnership(activity.getOrganization().getId());
            }
            activityService.cancelActivity(id);
            redirectAttributes.addFlashAttribute("success", "Actividad cancelada exitosamente");
        } catch (SecurityException e) {
            log.warn("User attempted to cancel activity from different organization: {}", id);
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (IllegalStateException | IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/activities";
    }
    @GetMapping("/{id}")
    public String viewActivity(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        log.debug("Viewing activity detail: {}", id);
        try {
            Activity activity = activityService.getActivityById(id);
            if (activity.getOrganization() != null) {
                securityUtils.validateResourceOwnership(activity.getOrganization().getId());
            }
            long sessionsCount = sessionService.getSessionsByActivity(id).size();
            long enrolledCount = enrollmentService.getEnrolledCount(id);
            model.addAttribute("activity", activity);
            model.addAttribute("sessionsCount", sessionsCount);
            model.addAttribute("enrolledCount", enrolledCount);
            model.addAttribute("canManage", securityUtils.canManageActivities());
            model.addAttribute("activeMenu", "activities");
            return "activities/detail";
        } catch (SecurityException e) {
            log.warn("User attempted to view activity from different organization: {}", id);
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/activities";
        }
    }
    @GetMapping("/{id}/sessions")
    public String viewSchedule(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        log.debug("Viewing schedule for activity: {}", id);
        try {
            Activity activity = activityService.getActivityById(id);
            if (activity.getOrganization() != null) {
                securityUtils.validateResourceOwnership(activity.getOrganization().getId());
            }
            List<Session> sessions = sessionService.getSessionsByActivity(id);
            model.addAttribute("activity", activity);
            model.addAttribute("sessions", sessions);
            model.addAttribute("activeMenu", "activities");
            return "activities/schedule";
        } catch (SecurityException e) {
            log.warn("User attempted to view schedule from different organization: {}", id);
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/activities";
        }
    }
    @GetMapping("/{id}/attendances")
    public String viewAttendances(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        log.debug("Viewing attendances for activity: {}", id);
        try {
            Activity activity = activityService.getActivityById(id);
            if (activity.getOrganization() != null) {
                securityUtils.validateResourceOwnership(activity.getOrganization().getId());
            }
            List<Session> sessions = sessionService.getSessionsByActivity(id);
            Map<Session, List<Attendance>> attendanceMap = sessions.stream()
                    .collect(Collectors.toMap(s -> s, s -> attendanceService.getAttendanceBySession(s.getId())));
            model.addAttribute("activity", activity);
            model.addAttribute("attendanceMap", attendanceMap);
            model.addAttribute("activeMenu", "activities");
            return "activities/attendances";
        } catch (SecurityException e) {
            log.warn("User attempted to view attendances from different organization: {}", id);
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/activities";
        }
    }
    @GetMapping("/{id}/cancel-confirm")
    public String showCancelConfirmation(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        log.debug("Showing cancel confirmation for activity: {}", id);
        if (!securityUtils.canManageActivities()) {
            redirectAttributes.addFlashAttribute("error", "No tiene permisos para cancelar actividades");
            return "redirect:/activities";
        }
        try {
            Activity activity = activityService.getActivityById(id);
            if (activity.getOrganization() != null) {
                securityUtils.validateResourceOwnership(activity.getOrganization().getId());
            }
            long pendingSessions = sessionService.getSessionsByActivity(id).stream()
                    .filter(s -> s.getStatus() == SessionStatus.PLANNED)
                    .count();
            model.addAttribute("activity", activity);
            model.addAttribute("pendingSessions", pendingSessions);
            return "activities/cancel-confirm";
        } catch (SecurityException e) {
            log.warn("User attempted to view cancel confirmation from different organization: {}", id);
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/activities";
        }
    }
    @GetMapping("/search")
    @ResponseBody
    public List<Activity> searchActivities(@RequestParam String q) {
        log.debug("Searching activities with query: {}", q);
        User currentUser = securityUtils.getCurrentUser()
                .orElseThrow(() -> new IllegalStateException("Usuario no autenticado"));
        String role = securityUtils.hasRole("ADMIN") ? "ADMIN" :
                securityUtils.hasRole("ORG_OWNER") ? "ORG_OWNER" :
                        securityUtils.hasRole("ORG_ADMIN") ? "ORG_ADMIN" : "ORG_MEMBER";
        return activityService.searchActivities(q, currentUser.getId(), role);
    }
}
