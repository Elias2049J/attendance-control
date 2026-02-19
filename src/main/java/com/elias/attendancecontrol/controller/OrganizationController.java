package com.elias.attendancecontrol.controller;
import com.elias.attendancecontrol.config.SecurityUtils;
import com.elias.attendancecontrol.model.dto.OrganizationRegistrationDTO;
import com.elias.attendancecontrol.model.dto.OrganizationStatsDTO;
import com.elias.attendancecontrol.model.entity.Organization;
import com.elias.attendancecontrol.model.entity.OrganizationPlan;
import com.elias.attendancecontrol.model.entity.SystemRole;
import com.elias.attendancecontrol.model.entity.User;
import com.elias.attendancecontrol.service.LogService;
import com.elias.attendancecontrol.service.OrganizationService;
import com.elias.attendancecontrol.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.List;

@Slf4j
@Controller
@RequestMapping("/organizations")
@RequiredArgsConstructor
public class OrganizationController {
    private final OrganizationService organizationService;
    private final UserService userService;
    private final SecurityUtils securityUtils;
    private final LogService logService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public String listOrganizations(Model model) {
        log.debug("Listing all organizations for ADMIN");
        try {
            List<Organization> organizations = organizationService.findAll();
            model.addAttribute("organizations", organizations);
            model.addAttribute("activeMenu", "organizations-list");
            return "organizations/list";
        } catch (Exception e) {
            log.error("Error listing organizations: {}", e.getMessage());
            model.addAttribute("error", e.getMessage());
            return "organizations/list";
        }
    }
    @GetMapping("/{id}/members")
    @PreAuthorize("hasRole('ADMIN')")
    public String showOrganizationMembers(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        log.debug("Showing members of organization: {}", id);
        try {
            Organization organization = organizationService.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Organización no encontrada"));
            List<User> members = userService.findByOrganizationId(id);
            model.addAttribute("organization", organization);
            model.addAttribute("members", members);
            model.addAttribute("activeMenu", "organizations-list");
            return "organizations/members";
        } catch (Exception e) {
            log.error("Error showing organization members: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/organizations";
        }
    }
    @GetMapping("/register")
    public String showRegisterForm(Model model) {
        log.debug("Showing organization registration form");
        model.addAttribute("registrationDTO", new OrganizationRegistrationDTO());
        model.addAttribute("plans", OrganizationPlan.values());
        return "organizations/register";
    }
    @PostMapping("/register")
    public String registerOrganization(@ModelAttribute OrganizationRegistrationDTO registrationDTO,
                                      HttpServletRequest request,
                                      Model model,
                                      RedirectAttributes redirectAttributes) {
        Organization organization = registrationDTO.getOrganization();
        User user = registrationDTO.getUser();
        log.debug("Registering new organization: {} with owner: {}",
                  organization != null ? organization.getName() : "null",
                  user != null ? user.getUsername() : "null");
        if (organization == null || organization.getName() == null || organization.getName().isBlank() ||
            organization.getSlug() == null || organization.getSlug().isBlank() ||
            organization.getPlan() == null) {
            log.error("Organization data incomplete");
            model.addAttribute("error", "Todos los campos de la organización son obligatorios");
            model.addAttribute("plans", OrganizationPlan.values());
            model.addAttribute("registrationDTO", registrationDTO);
            return "organizations/register";
        }
        if (user == null || user.getUsername() == null || user.getUsername().isBlank() ||
            user.getEmail() == null || user.getEmail().isBlank() ||
            user.getName() == null || user.getName().isBlank() ||
            user.getLastname() == null || user.getLastname().isBlank() ||
            user.getPassword() == null || user.getPassword().isBlank()) {
            log.error("User data incomplete");
            model.addAttribute("error", "Todos los campos del administrador son obligatorios");
            model.addAttribute("plans", OrganizationPlan.values());
            model.addAttribute("registrationDTO", registrationDTO);
            return "organizations/register";
        }
        try {
            user.setSystemRole(SystemRole.USER);
            user.setActive(true);
            User savedUser = userService.registerUser(user);
            Organization savedOrg = organizationService.registerOrganization(organization, savedUser);
            logService.log(builder -> builder
                .eventType("ORGANIZATION_REGISTERED")
                .description("Nueva organización registrada: " + savedOrg.getName())
                .user(savedUser)
                .ipAddress(request.getRemoteAddr())
                .details("Plan: " + savedOrg.getPlan() + ", Slug: " + savedOrg.getSlug())
            );
            redirectAttributes.addFlashAttribute("success",
                "¡Organización registrada exitosamente! Ya puedes iniciar sesión.");
            return "redirect:/auth/login";
        } catch (IllegalArgumentException e) {
            log.error("Error registering organization: {}", e.getMessage());
            logService.log(builder -> builder
                .eventType("ORGANIZATION_REGISTRATION_FAILED")
                .description("Error al registrar organización: " +
                        organization.getName())
                .ipAddress(request.getRemoteAddr())
                .details("Error: " + e.getMessage())
            );
            model.addAttribute("error", e.getMessage());
            model.addAttribute("plans", OrganizationPlan.values());
            model.addAttribute("registrationDTO", registrationDTO);
            return "organizations/register";
        }
    }
    @GetMapping("/manage")
    public String manageOrganization(Model model) {
        log.debug("Showing organization management panel");
        try {
            User currentUser = securityUtils.getCurrentUser().orElseThrow();
            if (currentUser.getOrganization() == null) {
                return "redirect:/";
            }
            Organization org = currentUser.getOrganization();
            model.addAttribute("organization", org);
            model.addAttribute("userCount", organizationService.getUserCount(org.getId()));
            model.addAttribute("activityCount", organizationService.getActivityCount(org.getId()));
            model.addAttribute("currentUser", currentUser);
            return "organizations/manage";
        } catch (Exception e) {
            log.error("Error loading organization management: {}", e.getMessage());
            return "redirect:/";
        }
    }
    @GetMapping("/edit")
    public String showEditForm(Model model) {
        log.debug("Showing organization edit form");
        try {
            User currentUser = securityUtils.getCurrentUser().orElseThrow();
            Organization org = currentUser.getOrganization();
            if (org == null) {
                return "redirect:/";
            }
            model.addAttribute("organization", org);
            model.addAttribute("plans", OrganizationPlan.values());
            return "organizations/edit";
        } catch (Exception e) {
            log.error("Error showing edit form: {}", e.getMessage());
            return "redirect:/organizations/manage";
        }
    }
    @PostMapping("/update")
    public String updateOrganization(@Valid @ModelAttribute Organization organization,
                                    BindingResult result,
                                    HttpServletRequest request,
                                    Model model,
                                    RedirectAttributes redirectAttributes) {
        log.debug("Updating organization: {}", organization.getId());
        if (result.hasErrors()) {
            model.addAttribute("plans", OrganizationPlan.values());
            return "organizations/edit";
        }
        try {
            User currentUser = securityUtils.getCurrentUserOrThrow();
            Organization currentOrg = currentUser.getOrganization();
            if (currentOrg == null || !currentOrg.getId().equals(organization.getId())) {
                log.warn("User {} attempted to update organization {} without permission",
                        currentUser.getUsername(), organization.getId());
                logService.log(builder -> builder
                    .eventType("UNAUTHORIZED_ORG_UPDATE_ATTEMPT")
                    .description("Intento no autorizado de actualizar organización")
                    .user(currentUser)
                    .ipAddress(request.getRemoteAddr())
                    .details("Target org ID: " + organization.getId())
                );
                redirectAttributes.addFlashAttribute("error", "No tienes permisos para editar esta organización");
                return "redirect:/organizations/manage";
            }
            if (!securityUtils.isOrganizationOwner()) {
                log.warn("Non-owner user {} attempted to update organization",
                        currentUser.getUsername());
                logService.log(builder -> builder
                    .eventType("UNAUTHORIZED_ORG_UPDATE_ATTEMPT")
                    .description("Usuario sin permisos de propietario intentó actualizar organización")
                    .user(currentUser)
                    .organization(currentOrg)
                    .ipAddress(request.getRemoteAddr())
                );
                redirectAttributes.addFlashAttribute("error", "Solo el propietario puede actualizar la organización");
                return "redirect:/organizations/manage";
            }
            organizationService.updateOrganization(organization.getId(), organization);
            logService.log(builder -> builder
                .eventType("ORGANIZATION_UPDATED")
                .description("Organización actualizada: " + organization.getName())
                .user(currentUser)
                .organization(currentOrg)
                .ipAddress(request.getRemoteAddr())
            );
            redirectAttributes.addFlashAttribute("success", "Organización actualizada exitosamente");
            return "redirect:/organizations/manage";
        } catch (Exception e) {
            log.error("Error updating organization: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/organizations/manage";
        }
    }
    @GetMapping("/dashboard")
    public String showDashboard(Model model, RedirectAttributes redirectAttributes) {
        log.debug("Showing organization dashboard");
        try {
            User currentUser = securityUtils.getCurrentUser()
                    .orElseThrow(() -> new IllegalStateException("Usuario no autenticado"));
            if (currentUser.getOrganization() == null) {
                redirectAttributes.addFlashAttribute("error", "No pertenece a ninguna organización");
                return "redirect:/";
            }
            Organization org = currentUser.getOrganization();
            OrganizationStatsDTO stats = organizationService.getStatsByOrganization(org.getId());
            model.addAttribute("organization", org);
            model.addAttribute("stats", stats);
            model.addAttribute("activeMenu", "organizations");
            return "organizations/dashboard";
        } catch (Exception e) {
            log.error("Error loading dashboard", e);
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/";
        }
    }
    @GetMapping("/members")
    public String listMembers(RedirectAttributes redirectAttributes) {
        log.debug("Redirecting to users list");
        return "redirect:/users";
    }
    @GetMapping("/settings")
    public String showSettings(Model model, RedirectAttributes redirectAttributes) {
        log.debug("Showing organization settings");
        try {
            User currentUser = securityUtils.getCurrentUser()
                    .orElseThrow(() -> new IllegalStateException("Usuario no autenticado"));
            if (currentUser.getOrganization() == null) {
                redirectAttributes.addFlashAttribute("error", "No pertenece a ninguna organización");
                return "redirect:/";
            }
            Organization org = currentUser.getOrganization();
            model.addAttribute("organization", org);
            model.addAttribute("activeMenu", "organizations");
            return "organizations/settings";
        } catch (Exception e) {
            log.error("Error loading settings", e);
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/";
        }
    }
}
