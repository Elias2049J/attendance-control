package com.elias.attendancecontrol.controller;
import com.elias.attendancecontrol.config.SecurityUtils;
import com.elias.attendancecontrol.model.entity.Activity;
import com.elias.attendancecontrol.service.*;
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
    private final OrganizationService organizationService;

    @GetMapping("/")
    public String home(Model model) {
        var userOptional = securityUtils.getCurrentUser();
        if (userOptional.isEmpty()) {
            log.debug("No authenticated user, showing landing page");
            return "landing";
        }
        var user = userOptional.get();
        log.debug("User {} accessing dashboard", user.getUsername());
        model.addAttribute("username", user.getUsername());
        model.addAttribute("user", user);
        model.addAttribute("organization", user.getOrganization());
        model.addAttribute("isSystemAdmin", securityUtils.isSystemAdmin());
        model.addAttribute("isOrgOwner", securityUtils.isOrganizationOwner());
        model.addAttribute("isOrgAdmin", securityUtils.isOrganizationAdmin());
        model.addAttribute("canManageUsers", securityUtils.canManageUsers());
        model.addAttribute("canManageActivities", securityUtils.canManageActivities());
        try {

            List<Activity> activities = List.of();
            long activitiesCount = 0;
            long totalUsers = 0;
            List<Activity> enrolledActivities = enrollmentService.getActivitiesByUser(user.getId());
            List<Activity> responsibleActivities = activityService.findByResponsible(user.getId());
            if (securityUtils.isOrganizationOwnerOrAdmin()) {
                activities = activityService.listActivitiesSorted();
                activitiesCount = activities.size();
                totalUsers = organizationService.getUserCount(securityUtils.getCurrentOrganizationId().orElse(null));
            } else {
                activitiesCount = enrolledActivities.size() + responsibleActivities.size();
                totalUsers = userService.countAllUsers();
            }
            if (securityUtils.isSystemAdmin()) {
                activitiesCount = activityService.countAll();
            }
            model.addAttribute("totalUsers", totalUsers);
            model.addAttribute("activities", activities);
            model.addAttribute("responsibleActivities", responsibleActivities);
            model.addAttribute("enrolledActivities", enrolledActivities);
            model.addAttribute("totalActivities", activitiesCount);
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

    @GetMapping("/terms")
    public String terms() {
        return "legal/terms";
    }

    @GetMapping("/privacy")
    public String privacy() {
        return "legal/privacy";
    }

    @GetMapping("/support")
    public String support() {
        return "legal/support";
    }
}
