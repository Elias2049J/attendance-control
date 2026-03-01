package com.elias.attendancecontrol.controller;
import com.elias.attendancecontrol.config.SecurityUtils;
import com.elias.attendancecontrol.model.entity.*;
import com.elias.attendancecontrol.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
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
    private final TokenService tokenService;
    private final SecurityUtils securityUtils;

    @Value("${app.base-url}")
    private String baseUrl;

    @Value("${qr.duration-minutes}")
    private int qrDurationMinutes;

    @GetMapping
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ORG_MEMBER')")
    public String listActivities(Model model) {
        User user = securityUtils.getCurrentUserOrThrow();
        log.debug("Listing all activities");
        List<Activity> allActivities = new ArrayList<>();
        if (securityUtils.isOrganizationOwnerOrAdmin()){
             allActivities = activityService.listActivitiesSorted();
        }
        model.addAttribute("responsibleActivities", activityService.findByResponsible(user.getId()));
        model.addAttribute("enrolledActivities", enrollmentService.getActivitiesByUser(user.getId()));
        model.addAttribute("activities", allActivities);
        model.addAttribute("activeMenu", "activities");
        return "activities/list";
    }

    @GetMapping("/new")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN')")
    public String showCreateForm(Model model) {
        log.debug("Showing activity creation form");
        model.addAttribute("activity", new Activity());
        model.addAttribute("users", userService.listUsers());
        model.addAttribute("recurrenceTypes", RecurrenceType.values());
        model.addAttribute("daysOfWeek", DayOfWeek.values());
        model.addAttribute("activeMenu", "activities");
        return "activities/form";
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN')")
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
            activity.setResponsible(userService.getUserById(responsibleId));

            RecurrenceRule recurrenceRule = new RecurrenceRule();
            recurrenceRule.setRecurrenceType(RecurrenceType.valueOf(recurrenceTypeStr));
            recurrenceRule.setStartDate(startDate);
            recurrenceRule.setEndDate(endDate);
            recurrenceRule.setDaysOfWeek(daysOfWeek);
            recurrenceRule.setStartTime(startTime);
            recurrenceRule.setEndTime(endTime);
            recurrenceRule.setToleranceMinutes(toleranceMinutes);

            activityService.createActivityWithRecurrence(activity, recurrenceRule);

            redirectAttributes.addFlashAttribute("success", "Actividad creada exitosamente");
            return "redirect:/activities";
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.error("Error creating activity: {}", e.getMessage());
            model.addAttribute("error", e.getMessage());
            model.addAttribute("activity", activity);
            model.addAttribute("users", userService.listUsers());
            model.addAttribute("recurrenceTypes", RecurrenceType.values());
            model.addAttribute("daysOfWeek", DayOfWeek.values());
            model.addAttribute("activeMenu", "activities");
            return "activities/form";
        }
    }

    @GetMapping("/{id}/edit")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN')")
    public String showEditForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        log.debug("Showing edit form for activity: {}", id);
        try {
            Activity activity = activityService.getActivityById(id);
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
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN')")
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
                activity.setResponsible(userService.getUserById(responsibleId));
            }

            RecurrenceRule recurrenceRule = existingActivity.getRecurrenceRule() != null
                    ? existingActivity.getRecurrenceRule()
                    : new RecurrenceRule();
            recurrenceRule.setRecurrenceType(RecurrenceType.valueOf(recurrenceTypeStr));
            recurrenceRule.setStartDate(startDate);
            recurrenceRule.setEndDate(endDate);
            recurrenceRule.setDaysOfWeek(daysOfWeek);
            recurrenceRule.setStartTime(startTime);
            recurrenceRule.setEndTime(endTime);
            recurrenceRule.setToleranceMinutes(toleranceMinutes);

            activityService.updateActivityWithRecurrence(id, activity, recurrenceRule);

            redirectAttributes.addFlashAttribute("success", "Actividad actualizada exitosamente");
            return "redirect:/activities";
        } catch (SecurityException e) {
            log.warn("User attempted to update activity from different organization: {}", id);
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/activities";
        } catch (IllegalArgumentException e) {
            log.error("Error updating activity: {}", e.getMessage());
            model.addAttribute("error", e.getMessage());
            model.addAttribute("activity", activity);
            model.addAttribute("users", userService.listUsers());
            model.addAttribute("recurrenceTypes", RecurrenceType.values());
            model.addAttribute("daysOfWeek", DayOfWeek.values());
            model.addAttribute("isEdit", true);
            model.addAttribute("activeMenu", "activities");
            return "activities/form";
        }
    }

    @PostMapping("/{id}/activate")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN')")
    public String activateActivity(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        log.debug("Activating activity: {}", id);
        try {
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
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN')")
    public String pauseActivity(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        log.debug("Pausing activity: {}", id);
        try {
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
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN')")
    public String completeActivity(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        log.debug("Completing activity: {}", id);
        try {
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
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN')")
    public String cancelActivity(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        log.debug("Cancelling activity: {}", id);
        try {
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
            long userId = securityUtils.getCurrentUserOrThrow().getId();
            long sessionsCount = sessionService.getSessionsByActivity(id).size();
            long enrolledCount = enrollmentService.getEnrolledCount(id);
            model.addAttribute("activity", activity);
            model.addAttribute("sessionsCount", sessionsCount);
            model.addAttribute("enrolledCount", enrolledCount);
            model.addAttribute("canManage", securityUtils.canManageActivities());
            model.addAttribute("activeMenu", "activities");
            model.addAttribute("isEnrolled", enrollmentService.isUserEnrolled(id, userId));
            model.addAttribute("isResponsible", activityService.isResponsible(id, userId));
            model.addAttribute("attendancesCount", attendanceService.getAttendanceCountByUserIdAndActivityId(id, userId));
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

    @GetMapping("/{activityId}/sessions/{sessionId}/manage")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ORG_MEMBER')")
    public String manageSession(@PathVariable Long activityId,
                               @PathVariable Long sessionId,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        log.debug("Managing session {} from activity {}", sessionId, activityId);
        try {
            Activity activity = activityService.getActivityById(activityId);

            Session session = sessionService.getSessionById(sessionId);
            if (!session.getActivity().getId().equals(activityId)) {
                throw new IllegalArgumentException("La sesión no pertenece a esta actividad");
            }

            List<Attendance> attendances = attendanceService.getAttendanceBySession(sessionId);
            List<Enrollment> enrollments = enrollmentService.getEnrollmentsByActivity(activityId);
            List<Enrollment> enrollmentsWithoutAttendance = enrollmentService.getEnrollmentsWithoutAttendance(activityId, attendances);

            model.addAttribute("activity", activity);
            model.addAttribute("sessionEntity", session);
            model.addAttribute("attendances", attendances);
            model.addAttribute("enrollments", enrollments);
            model.addAttribute("enrollmentsWithoutAttendance", enrollmentsWithoutAttendance);

            return "sessions/manage";
        } catch (SecurityException e) {
            log.warn("User attempted to access session from different organization: activityId={}, sessionId={}",
                    activityId, sessionId);
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/activities";
        } catch (IllegalArgumentException e) {
            log.error("Error accessing session: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/activities/" + activityId + "/sessions";
        }
    }

    @GetMapping("/{activityId}/sessions/{sessionId}/manage/qr")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ORG_MEMBER')")
    public String viewSessionQR(@PathVariable Long activityId,
                                @PathVariable Long sessionId,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        log.debug("Viewing QR for session {} from activity {}", sessionId, activityId);
        try {
            Activity activity = activityService.getActivityById(activityId);


            Session sessionEntity = sessionService.getSessionById(sessionId);
            if (!sessionEntity.getActivity().getId().equals(activityId)) {
                throw new IllegalArgumentException("La sesión no pertenece a esta actividad");
            }

            if (!sessionEntity.getStatus().isActive()) {
                redirectAttributes.addFlashAttribute("error", "La sesión debe estar activa para ver el código QR");
                return "redirect:/activities/" + activityId + "/sessions/" + sessionId + "/manage";
            }

            Map<String, Object> qrData = tokenService.generateQRWithFullData(sessionId, baseUrl);

            model.addAllAttributes(qrData);
            model.addAttribute("activity", activity);
            model.addAttribute("sessionEntity", sessionEntity);
            model.addAttribute("qrDurationMinutes", qrDurationMinutes);
            return "qr/view";
        } catch (SecurityException e) {
            log.warn("User attempted to access QR from different organization: activityId={}, sessionId={}",
                    activityId, sessionId);
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/activities";
        } catch (IllegalArgumentException e) {
            log.error("Error accessing QR: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/activities/" + activityId + "/sessions/" + sessionId + "/manage";
        }
    }

    @PostMapping("/{activityId}/sessions/{sessionId}/manage/qr/regenerate")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ORG_MEMBER')")
    public String regenerateSessionQR(@PathVariable Long activityId,
                                     @PathVariable Long sessionId,
                                     RedirectAttributes redirectAttributes) {
        log.debug("Regenerating QR for session {} from activity {}", sessionId, activityId);
        try {
            activityService.getActivityById(activityId);

            tokenService.regenerateQR(sessionId);
            redirectAttributes.addFlashAttribute("success", "Código QR regenerado exitosamente");
            return "redirect:/activities/" + activityId + "/sessions/" + sessionId + "/manage/qr";
        } catch (SecurityException e) {
            log.warn("User attempted to regenerate QR from different organization: activityId={}, sessionId={}",
                    activityId, sessionId);
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/activities";
        } catch (IllegalArgumentException e) {
            log.error("Error regenerating QR: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/activities/" + activityId + "/sessions/" + sessionId + "/manage";
        }
    }
}

