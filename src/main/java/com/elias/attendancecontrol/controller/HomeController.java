package com.elias.attendancecontrol.controller;
import com.elias.attendancecontrol.config.SecurityUtils;
import com.elias.attendancecontrol.model.entity.Activity;
import com.elias.attendancecontrol.service.ActivityService;
import com.elias.attendancecontrol.service.EnrollmentService;
import com.elias.attendancecontrol.service.SessionService;
import com.elias.attendancecontrol.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import java.util.List;
@Slf4j
@Controller
@RequiredArgsConstructor
public class HomeController {
    private final UserService userService;
    private final ActivityService activityService;
    private final SessionService sessionService;
    private final EnrollmentService enrollmentService;
    private final SecurityUtils securityUtils;
    @GetMapping("/")
    public String home(Model model) {
        var userOptional = securityUtils.getCurrentUser();
        if (userOptional.isEmpty()) {
            log.warn("No authenticated user found in security context, redirecting to login");
            return "redirect:/auth/login";
        }
        var user = userOptional.get();
        log.debug("User {} accessing home page", user.getUsername());
        model.addAttribute("username", user.getUsername());
        model.addAttribute("user", user);
        model.addAttribute("organization", user.getOrganization());
        model.addAttribute("isSystemAdmin", securityUtils.isSystemAdmin());
        model.addAttribute("isOrgOwner", securityUtils.isOrganizationOwner());
        model.addAttribute("isOrgAdmin", securityUtils.isOrganizationAdmin());
        model.addAttribute("canManageUsers", securityUtils.canManageUsers());
        model.addAttribute("canManageActivities", securityUtils.canManageActivities());
        try {
            model.addAttribute("totalUsers", userService.listUsers().size());
            List<Activity> activities;
            if (securityUtils.hasRole("ORG_MEMBER")) {
                activities = enrollmentService.getActivitiesByUser(user.getId());
            } else {
                activities = activityService.listActivities();
            }
            model.addAttribute("activities", activities);
            model.addAttribute("totalActivities", activities.size());
            model.addAttribute("totalSessions", sessionService.listSessions().size());
        } catch (Exception e) {
            log.warn("Error loading dashboard statistics", e);
            model.addAttribute("totalUsers", 0);
            model.addAttribute("activities", List.of());
            model.addAttribute("totalActivities", 0);
            model.addAttribute("totalSessions", 0);
        }
        model.addAttribute("activeMenu", "home");
        return "index";
    }
}
