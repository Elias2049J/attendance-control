package com.elias.attendancecontrol.config;
import com.elias.attendancecontrol.model.entity.Organization;
import com.elias.attendancecontrol.model.entity.OrganizationRole;
import com.elias.attendancecontrol.model.entity.SystemRole;
import com.elias.attendancecontrol.model.entity.User;
import com.elias.attendancecontrol.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class SecurityUtils {
    private final UserService userService;

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

    public boolean isOrganizationMember() {
        return getCurrentUser()
                .map(user -> user.getOrganizationRole() != null && user.getOrganizationRole().isMember())
                .orElse(false);
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

    public boolean canManageUsers() {
        return isSystemAdmin() || isOrganizationOwnerOrAdmin();
    }

    public boolean canManageActivities() {
        return isSystemAdmin() || isOrganizationOwnerOrAdmin();
    }

    public boolean canEditUser(User targetUser) {
        User currentUser = getCurrentUser().orElse(null);
        if (currentUser == null) return false;
        if (isSystemAdmin()) return true;
        OrganizationRole targetRole = targetUser.getOrganizationRole();
        if (isOrganizationAdmin() && targetRole != null && targetRole.isAdmin()) return false;
        if (isOrganizationOwnerOrAdmin() && currentUser.getOrganization() != null
                && targetUser.getOrganization() != null
                && currentUser.getOrganization().getId().equals(targetUser.getOrganization().getId())) {
            return targetRole != OrganizationRole.OWNER;
        }
        return false;
    }


    public void refreshCurrentUserInSession(Long userId) {
        try {
            Authentication currentAuth = SecurityContextHolder.getContext().getAuthentication();
            if (currentAuth == null) return;
            Object principal = currentAuth.getPrincipal();
            if (!(principal instanceof CustomUserDetails currentDetails)) return;
            if (!currentDetails.getUser().getId().equals(userId)) return;

            User freshUser = userService.getUserById(userId);

            var authorities = new ArrayList<SimpleGrantedAuthority>();
            authorities.add(new SimpleGrantedAuthority("ROLE_" + freshUser.getSystemRole().name()));
            if (freshUser.getOrganizationRole() != null) {
                authorities.add(new SimpleGrantedAuthority("ROLE_ORG_" + freshUser.getOrganizationRole().name()));
            }
            CustomUserDetails freshDetails = new CustomUserDetails(freshUser, authorities);
            UsernamePasswordAuthenticationToken newAuth = new UsernamePasswordAuthenticationToken(
                    freshDetails, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(newAuth);
            log.debug("SecurityContext refreshed for user: {}", freshUser.getUsername());
        } catch (Exception e) {
            log.warn("Could not refresh SecurityContext for user {}: {}", userId, e.getMessage());
        }
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


    public boolean hasRole(String role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> Objects.equals(authority.getAuthority(), "ROLE_" + role));
    }
}
