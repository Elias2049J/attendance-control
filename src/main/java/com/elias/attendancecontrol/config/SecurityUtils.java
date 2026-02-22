package com.elias.attendancecontrol.config;
import com.elias.attendancecontrol.model.entity.Organization;
import com.elias.attendancecontrol.model.entity.OrganizationRole;
import com.elias.attendancecontrol.model.entity.SystemRole;
import com.elias.attendancecontrol.model.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;
@Slf4j
@Component
public class SecurityUtils {
    public Optional<User> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof CustomUserDetails userDetails) {
            return Optional.of(userDetails.getUser());
        }
        return Optional.empty();
    }
    public User getCurrentUserOrThrow() {
        return getCurrentUser()
                .orElseThrow(() -> new IllegalStateException("Usuario no autenticado"));
    }

    public Optional<Long> getCurrentOrganizationId() {
        return getCurrentUser()
                .map(User::getOrganization)
                .map(Organization::getId);
    }
    
    public Optional<Organization> getCurrentOrganization() {
        return getCurrentUser().map(User::getOrganization);
    }

    public boolean isSystemAdmin() {
        return getCurrentUser()
                .map(user -> user.getSystemRole() == SystemRole.ADMIN && user.getOrganization() == null)
                .orElse(false);
    }
    public boolean hasSystemRole(SystemRole systemRole) {
        return getCurrentUser()
                .map(user -> user.getSystemRole() == systemRole)
                .orElse(false);
    }
    public boolean hasOrganizationRole(OrganizationRole organizationRole) {
        return getCurrentUser()
                .map(user -> user.getOrganizationRole() == organizationRole)
                .orElse(false);
    }
    public boolean isOrganizationOwner() {
        return hasOrganizationRole(OrganizationRole.OWNER);
    }
    public boolean isOrganizationAdmin() {
        return hasOrganizationRole(OrganizationRole.ADMIN);
    }
    public boolean isOrganizationOwnerOrAdmin() {
        return isOrganizationOwner() || isOrganizationAdmin();
    }
    public boolean belongsToOrganization(Long organizationId) {
        return getCurrentUser()
                .map(user -> user.getOrganization() != null
                        && user.getOrganization().getId().equals(organizationId))
                .orElse(false);
    }
    public boolean canManageUsers() {
        return isSystemAdmin() || isOrganizationOwnerOrAdmin();
    }
    public boolean canManageActivities() {
        return isSystemAdmin() || isOrganizationOwnerOrAdmin();
    }
    public boolean canViewUser(User targetUser) {
        User currentUser = getCurrentUser().orElse(null);
        if (currentUser == null) return false;
        if (isSystemAdmin()) return true;
        if (currentUser.getId().equals(targetUser.getId())) return true;
        return isOrganizationOwnerOrAdmin() && currentUser.getOrganization() != null
                && targetUser.getOrganization() != null
                && currentUser.getOrganization().getId().equals(targetUser.getOrganization().getId());
    }
    public boolean canEditUser(User targetUser) {
        User currentUser = getCurrentUser().orElse(null);
        if (currentUser == null) return false;
        if (isSystemAdmin()) return true;
        if (isOrganizationOwnerOrAdmin() && currentUser.getOrganization() != null
                && targetUser.getOrganization() != null
                && currentUser.getOrganization().getId().equals(targetUser.getOrganization().getId())) {
            return targetUser.getOrganizationRole() != OrganizationRole.OWNER;
        }
        return false;
    }
    public void requireSystemAdmin() {
        if (!isSystemAdmin()) {
            throw new SecurityException("Se requiere rol de administrador del sistema");
        }
    }
    public void requireOrganizationOwnerOrAdmin() {
        if (!isOrganizationOwnerOrAdmin()) {
            throw new SecurityException("Se requiere rol de propietario o administrador de organización");
        }
    }
    public void validateResourceOwnership(Long resourceOrgId) {
        if (isSystemAdmin()) {
            return;
        }
        Long currentOrgId = getCurrentOrganizationId().orElse(null);
        if (currentOrgId == null || !currentOrgId.equals(resourceOrgId)) {
            throw new SecurityException("No tiene permisos para acceder a este recurso");
        }
    }
    public boolean canAccessResource(Long resourceOrgId) {
        if (isSystemAdmin()) {
            return true;
        }
        Long currentOrgId = getCurrentOrganizationId().orElse(null);
        return currentOrgId != null && currentOrgId.equals(resourceOrgId);
    }
    public boolean hasRole(String role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> Objects.equals(authority.getAuthority(), "ROLE_" + role));
    }
}
