package com.elias.attendancecontrol.controller;
import com.elias.attendancecontrol.config.SecurityUtils;
import com.elias.attendancecontrol.model.entity.Activity;
import com.elias.attendancecontrol.model.entity.Enrollment;
import com.elias.attendancecontrol.model.entity.SystemRole;
import com.elias.attendancecontrol.model.entity.User;
import com.elias.attendancecontrol.service.ActivityService;
import com.elias.attendancecontrol.service.EnrollmentService;
import com.elias.attendancecontrol.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.List;
@Slf4j
@Controller
@RequestMapping("/activities/{activityId}/enrollments")
@RequiredArgsConstructor
public class EnrollmentController {
    private final EnrollmentService enrollmentService;
    private final ActivityService activityService;
    private final UserService userService;
    private final SecurityUtils securityUtils;
    @GetMapping
    public String listEnrollments(@PathVariable Long activityId, Model model, RedirectAttributes redirectAttributes) {
        log.debug("Listing enrollments for activity: {}", activityId);
        try {
            Activity activity = activityService.getActivityById(activityId);
            if (activity.getOrganization() != null) {
                securityUtils.validateResourceOwnership(activity.getOrganization().getId());
            }
            List<Enrollment> enrollments = enrollmentService.getEnrollmentsByActivity(activityId);
            List<User> enrolledUsers = enrollmentService.getEnrolledParticipants(activityId);
            long enrolledCount = enrollmentService.getEnrolledCount(activityId);
            model.addAttribute("activity", activity);
            model.addAttribute("enrollments", enrollments);
            model.addAttribute("enrolledUsers", enrolledUsers);
            model.addAttribute("enrolledCount", enrolledCount);
            return "enrollments/list";
        } catch (SecurityException e) {
            log.warn("User attempted to access enrollments from different organization: {}", activityId);
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/activities";
        }
    }
    @GetMapping("/enroll")
    public String showEnrollForm(@PathVariable Long activityId, Model model, RedirectAttributes redirectAttributes) {
        log.debug("Showing enroll form for activity: {}", activityId);
        try {
            Activity activity = activityService.getActivityById(activityId);
            if (activity.getOrganization() != null) {
                securityUtils.validateResourceOwnership(activity.getOrganization().getId());
            }
            List<User> enrolledUsers = enrollmentService.getEnrolledParticipants(activityId);
            List<Long> enrolledUserIds = enrolledUsers.stream()
                    .map(User::getId)
                    .toList();
            List<User> availableUsers = userService.getAvailableUsersExcluding(SystemRole.USER, enrolledUserIds);
            model.addAttribute("activity", activity);
            model.addAttribute("availableUsers", availableUsers);
            model.addAttribute("enrolledUsers", enrolledUsers);
            return "enrollments/enroll";
        } catch (SecurityException e) {
            log.warn("User attempted to access enroll form from different organization: {}", activityId);
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/activities";
        }
    }
    @PostMapping("/enroll-single")
    public String enrollSingleUser(@PathVariable Long activityId,
                                   @RequestParam Long userId,
                                   RedirectAttributes redirectAttributes) {
        log.debug("Enrolling single user {} in activity {}", userId, activityId);
        try {
            Activity activity = activityService.getActivityById(activityId);
            if (activity.getOrganization() != null) {
                securityUtils.validateResourceOwnership(activity.getOrganization().getId());
            }
            enrollmentService.enrollUser(activityId, userId);
            redirectAttributes.addFlashAttribute("success", "Usuario inscrito exitosamente");
        } catch (SecurityException e) {
            log.warn("User attempted to enroll from different organization: {}", activityId);
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            log.error("Error enrolling user: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Error al inscribir usuario: " + e.getMessage());
        }
        return "redirect:/activities/" + activityId + "/enrollments";
    }
    @PostMapping("/enroll-multiple")
    public String enrollMultipleUsers(@PathVariable Long activityId,
                                      @RequestParam(required = false) List<Long> userIds,
                                      RedirectAttributes redirectAttributes) {
        log.debug("Enrolling multiple users in activity {}", activityId);
        if (userIds == null || userIds.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Debe seleccionar al menos un usuario");
            return "redirect:/activities/" + activityId + "/enrollments/enroll";
        }
        try {
            Activity activity = activityService.getActivityById(activityId);
            if (activity.getOrganization() != null) {
                securityUtils.validateResourceOwnership(activity.getOrganization().getId());
            }
            enrollmentService.enrollMultipleUsers(activityId, userIds);
            redirectAttributes.addFlashAttribute("success",
                "Inscripción masiva completada. Total seleccionados: " + userIds.size());
        } catch (SecurityException e) {
            log.warn("User attempted to enroll multiple from different organization: {}", activityId);
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            log.error("Error enrolling multiple users: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Error en inscripción masiva: " + e.getMessage());
        }
        return "redirect:/activities/" + activityId + "/enrollments";
    }
    @PostMapping("/{userId}/remove")
    public String removeParticipant(@PathVariable Long activityId,
                                   @PathVariable Long userId,
                                   RedirectAttributes redirectAttributes) {
        log.debug("Removing user {} from activity {}", userId, activityId);
        try {
            Activity activity = activityService.getActivityById(activityId);
            if (activity.getOrganization() != null) {
                securityUtils.validateResourceOwnership(activity.getOrganization().getId());
            }
            enrollmentService.removeParticipant(activityId, userId);
            redirectAttributes.addFlashAttribute("success", "Participante removido exitosamente");
        } catch (SecurityException e) {
            log.warn("User attempted to remove participant from different organization: {}", activityId);
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            log.error("Error removing participant: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Error al remover participante: " + e.getMessage());
        }
        return "redirect:/activities/" + activityId + "/enrollments";
    }
    @GetMapping("/session/{sessionId}/absent")
    public String showAbsentUsers(@PathVariable Long activityId,
                                  @PathVariable Long sessionId,
                                  Model model,
                                  RedirectAttributes redirectAttributes) {
        log.debug("Showing absent users for session: {}", sessionId);
        try {
            Activity activity = activityService.getActivityById(activityId);
            if (activity.getOrganization() != null) {
                securityUtils.validateResourceOwnership(activity.getOrganization().getId());
            }
            List<User> expectedUsers = enrollmentService.getExpectedParticipants(sessionId);
            List<User> absentUsers = enrollmentService.getAbsentUsers(sessionId);
            model.addAttribute("activity", activity);
            model.addAttribute("sessionId", sessionId);
            model.addAttribute("expectedUsers", expectedUsers);
            model.addAttribute("absentUsers", absentUsers);
            model.addAttribute("expectedCount", expectedUsers.size());
            model.addAttribute("absentCount", absentUsers.size());
            model.addAttribute("attendanceRate",
                expectedUsers.isEmpty() ? 0 :
                ((expectedUsers.size() - absentUsers.size()) * 100.0 / expectedUsers.size()));
            return "enrollments/absent";
        } catch (SecurityException e) {
            log.warn("User attempted to access absent users from different organization: {}", activityId);
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/activities";
        } catch (Exception e) {
            log.error("Error showing absent users: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Error al cargar ausentes: " + e.getMessage());
            return "redirect:/activities/" + activityId + "/enrollments";
        }
    }
}
