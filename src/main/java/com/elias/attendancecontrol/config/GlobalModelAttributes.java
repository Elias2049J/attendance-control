package com.elias.attendancecontrol.config;

import com.elias.attendancecontrol.model.entity.Organization;
import com.elias.attendancecontrol.model.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@Slf4j
@ControllerAdvice
@RequiredArgsConstructor
public class GlobalModelAttributes {
    private final SecurityUtils securityUtils;

    @ModelAttribute("isSysAdmin")
    public Boolean isSysAdmin() {
        try {
            return securityUtils.isSystemAdmin();
        } catch (Exception e) {
            log.trace("Error checking if user is system admin", e);
            return false;
        }
    }

    @ModelAttribute("isOrgOwner")
    public Boolean isOrgOwner() {
        try {
            return securityUtils.isOrganizationOwner();
        } catch (Exception e) {
            log.trace("Error checking if user is org owner", e);
            return false;
        }
    }

    @ModelAttribute("isOrgMember")
    public Boolean isOrgMember() {
        try {
            return securityUtils.isOrganizationMember();
        } catch (Exception e) {
            log.trace("Error checking if user is org member", e);
            return false;
        }
    }

    @ModelAttribute("isOrgAdmin")
    public Boolean isOrgAdmin() {
        try {
            return securityUtils.isOrganizationAdmin();
        } catch (Exception e) {
            log.trace("Error checking if user is org admin", e);
            return false;
        }
    }

    @ModelAttribute("isOrgOwnerOrAdmin")
    public Boolean isOrgOwnerOrAdmin() {
        try {
            return securityUtils.isOrganizationOwnerOrAdmin();
        } catch (Exception e) {
            log.trace("Error checking if user is org owner or admin", e);
            return false;
        }
    }

    @ModelAttribute("canManageUsers")
    public Boolean canManageUsers() {
        try {
            return securityUtils.canManageUsers();
        } catch (Exception e) {
            log.trace("Error checking if user can manage users", e);
            return false;
        }
    }

    @ModelAttribute("canManageActivities")
    public Boolean canManageActivities() {
        try {
            return securityUtils.canManageActivities();
        } catch (Exception e) {
            log.trace("Error checking if user can manage activities", e);
            return false;
        }
    }

    @ModelAttribute("currentUsername")
    public String currentUsername() {
        return securityUtils.getCurrentUser()
                .map(User::getUsername)
                .orElse(null);
    }

    @ModelAttribute("currentUserFullName")
    public String currentUserFullName() {
        return securityUtils.getCurrentUser()
                .map(user -> user.getName() + " " + user.getLastname())
                .orElse(null);
    }

    @ModelAttribute("currentUser")
    public User currentUser() {
        return securityUtils.getCurrentUser().orElse(null);
    }

    @ModelAttribute("currentOrganizationName")
    public String currentOrganizationName() {
        return securityUtils.getCurrentOrganization()
                .map(Organization::getName)
                .orElse(null);
    }

    @ModelAttribute("currentOrganizationId")
    public Long currentOrganizationId() {
        return securityUtils.getCurrentOrganizationId().orElse(null);
    }

    @ModelAttribute("currentUserRoles")
    public String currentUserRoles() {
        return securityUtils.getCurrentUser()
                .map(user -> {
                    StringBuilder roles = new StringBuilder();
                    if (user.getSystemRole() != null) {
                        roles.append(user.getSystemRole().name());
                    }
                    if (user.getOrganizationRole() != null) {
                        if (!roles.isEmpty()) {
                            roles.append(", ");
                        }
                        roles.append("ORG_").append(user.getOrganizationRole().name());
                    }
                    return roles.toString();
                })
                .orElse("Sin rol");
    }
}
