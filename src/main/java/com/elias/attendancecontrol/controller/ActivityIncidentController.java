package com.elias.attendancecontrol.controller;
import com.elias.attendancecontrol.config.SecurityUtils;
import com.elias.attendancecontrol.model.entity.Activity;
import com.elias.attendancecontrol.model.entity.ActivityIncident;
import com.elias.attendancecontrol.service.ActivityIncidentService;
import com.elias.attendancecontrol.service.ActivityService;
import com.elias.attendancecontrol.service.SessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.time.LocalDate;
import java.util.List;
@Slf4j
@Controller
@RequestMapping("/incidents")
@RequiredArgsConstructor
public class ActivityIncidentController {
    private final ActivityIncidentService activityIncidentService;
    private final ActivityService activityService;
    private final SessionService sessionService;
    private final SecurityUtils securityUtils;
    @GetMapping
    public String listIncidents(Model model) {
        log.debug("Listing all incidents");
        List<ActivityIncident> incidents = activityIncidentService.getAllIncidents();
        model.addAttribute("incidents", incidents);
        return "incident/list";
    }
    @GetMapping("/activity/{activityId}")
    public String listIncidentsByActivity(@PathVariable Long activityId, Model model, RedirectAttributes redirectAttributes) {
        log.debug("Listing incidents for activity: {}", activityId);
        try {
            Activity activity = activityService.getActivityById(activityId);
            List<ActivityIncident> incidents = activityIncidentService.getIncidentsByActivity(activityId);
            model.addAttribute("activity", activity);
            model.addAttribute("incidents", incidents);
            return "incident/list";
        } catch (SecurityException e) {
            log.warn("User attempted to access incidents from different organization: {}", activityId);
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/activities";
        }
    }
    @GetMapping("/reschedule/{activityId}")
    public String showRescheduleForm(@PathVariable Long activityId, Model model, RedirectAttributes redirectAttributes) {
        log.debug("Showing reschedule form for activity: {}", activityId);
        try {
            Activity activity = activityService.getActivityById(activityId);
            model.addAttribute("activity", activity);
            model.addAttribute("sessions", sessionService.getSessionsByActivity(activityId));
            return "incident/reschedule";
        } catch (SecurityException e) {
            log.warn("User attempted to reschedule from different organization: {}", activityId);
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/activities";
        }
    }
    @PostMapping("/reschedule")
    public String rescheduleOccurrence(@RequestParam Long activityId,
                                      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate originalDate,
                                      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate newDate,
                                      @RequestParam String reason,
                                      RedirectAttributes redirectAttributes) {
        log.debug("Rescheduling occurrence for activity: {} from {} to {}", activityId, originalDate, newDate);
        try {
            activityIncidentService.rescheduleOccurrence(activityId, originalDate, newDate, reason);
            redirectAttributes.addFlashAttribute("success", "Ocurrencia reprogramada exitosamente");
            return "redirect:/activities";
        } catch (SecurityException e) {
            log.warn("User attempted to reschedule from different organization: {}", activityId);
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/activities";
        } catch (Exception e) {
            log.error("Error rescheduling occurrence: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Error: " + e.getMessage());
            return "redirect:/incidents/reschedule/" + activityId;
        }
    }
    @GetMapping("/cancel/{activityId}")
    public String showCancelForm(@PathVariable Long activityId, Model model, RedirectAttributes redirectAttributes) {
        log.debug("Showing cancel form for activity: {}", activityId);
        try {
            Activity activity = activityService.getActivityById(activityId);
            model.addAttribute("activity", activity);
            model.addAttribute("sessions", sessionService.getSessionsByActivity(activityId));
            return "incident/cancel";
        } catch (SecurityException e) {
            log.warn("User attempted to cancel from different organization: {}", activityId);
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/activities";
        }
    }
    @PostMapping("/cancel")
    public String cancelOccurrence(@RequestParam Long activityId,
                                   @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                   @RequestParam String reason,
                                   RedirectAttributes redirectAttributes) {
        log.debug("Cancelling occurrence for activity: {} on date: {}", activityId, date);
        try {
            activityIncidentService.cancelOccurrence(activityId, date, reason);
            redirectAttributes.addFlashAttribute("success", "Ocurrencia cancelada exitosamente");
            return "redirect:/activities";
        } catch (SecurityException e) {
            log.warn("User attempted to cancel from different organization: {}", activityId);
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/activities";
        } catch (Exception e) {
            log.error("Error cancelling occurrence: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Error: " + e.getMessage());
            return "redirect:/incidents/cancel/" + activityId;
        }
    }
    @GetMapping("/{id}")
    public String viewIncident(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        log.debug("Viewing incident: {}", id);
        try {
            ActivityIncident incident = activityIncidentService.getIncidentById(id);
            if (incident.getActivity().getOrganization() != null) {
                securityUtils.validateResourceOwnership(incident.getActivity().getOrganization().getId());
            }
            model.addAttribute("incident", incident);
            return "incident/view";
        } catch (SecurityException e) {
            log.warn("User attempted to view incident from different organization: {}", id);
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/incidents";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Incidencia no encontrada");
            return "redirect:/incidents";
        }
    }
}
