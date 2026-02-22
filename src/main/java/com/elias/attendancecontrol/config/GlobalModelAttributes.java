package com.elias.attendancecontrol.config;

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

    /**
     * Verifica si el usuario actual es administrador del sistema
     */
    @ModelAttribute("isSysAdmin")
    public boolean isSysAdmin() {
        return securityUtils.isSystemAdmin();
    }

    /**
     * Verifica si el usuario actual es dueño de organización
     */
    @ModelAttribute("isOrgOwner")
    public boolean isOrgOwner() {
        return securityUtils.isOrganizationOwner();
    }

    /**
     * Verifica si el usuario actual es administrador de organización
     */
    @ModelAttribute("isOrgAdmin")
    public boolean isOrgAdmin() {
        return securityUtils.isOrganizationAdmin();
    }

    /**
     * Verifica si el usuario actual es dueño o admin de organización
     */
    @ModelAttribute("isOrgOwnerOrAdmin")
    public boolean isOrgOwnerOrAdmin() {
        return securityUtils.isOrganizationOwnerOrAdmin();
    }

    /**
     * Verifica si el usuario puede gestionar usuarios
     */
    @ModelAttribute("canManageUsers")
    public boolean canManageUsers() {
        return securityUtils.canManageUsers();
    }

    /**
     * Verifica si el usuario puede gestionar actividades
     */
    @ModelAttribute("canManageActivities")
    public boolean canManageActivities() {
        return securityUtils.canManageActivities();
    }

    /**
     * Nombre de usuario actual
     */
    @ModelAttribute("currentUsername")
    public String currentUsername() {
        return securityUtils.getCurrentUser()
                .map(User::getUsername)
                .orElse(null);
    }

    /**
     * Nombre completo del usuario actual
     */
    @ModelAttribute("currentUserFullName")
    public String currentUserFullName() {
        return securityUtils.getCurrentUser()
                .map(user -> user.getName() + " " + user.getLastname())
                .orElse(null);
    }

    /**
     * Usuario actual completo (para casos específicos)
     */
    @ModelAttribute("currentUser")
    public User currentUser() {
        return securityUtils.getCurrentUser().orElse(null);
    }

    /**
     * Nombre de la organización del usuario actual
     */
    @ModelAttribute("currentOrganizationName")
    public String currentOrganizationName() {
        return securityUtils.getCurrentOrganization()
                .map(org -> org.getName())
                .orElse(null);
    }

    /**
     * ID de la organización del usuario actual
     */
    @ModelAttribute("currentOrganizationId")
    public Long currentOrganizationId() {
        return securityUtils.getCurrentOrganizationId().orElse(null);
    }
}
