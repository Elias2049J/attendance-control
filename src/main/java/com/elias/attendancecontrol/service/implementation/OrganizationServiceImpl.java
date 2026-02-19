package com.elias.attendancecontrol.service.implementation;
import com.elias.attendancecontrol.config.OrganizationPlanProperties;
import com.elias.attendancecontrol.model.dto.OrganizationStatsDTO;
import com.elias.attendancecontrol.model.entity.*;
import com.elias.attendancecontrol.persistence.repository.ActivityRepository;
import com.elias.attendancecontrol.persistence.repository.OrganizationRepository;
import com.elias.attendancecontrol.persistence.repository.SessionRepository;
import com.elias.attendancecontrol.persistence.repository.UserRepository;
import com.elias.attendancecontrol.service.LogService;
import com.elias.attendancecontrol.service.OrganizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
@Slf4j
@Service
@RequiredArgsConstructor
public class OrganizationServiceImpl implements OrganizationService {
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final ActivityRepository activityRepository;
    private final SessionRepository sessionRepository;
    private final LogService logService;
    private final OrganizationPlanProperties planProperties;
    @Override
    @Transactional
    public Organization registerOrganization(Organization organization, User owner) {
        log.debug("Registering new organization: {} with owner: {}", organization.getName(), owner.getUsername());
        if (organizationRepository.existsBySlug(organization.getSlug())) {
            throw new IllegalArgumentException("El identificador (slug) ya está en uso");
        }
        organization.setActive(true);
        organization.setCreatedDate(LocalDateTime.now());
        applyPlanLimitsFromConfig(organization);
        Organization savedOrg = organizationRepository.save(organization);
        owner.setOrganization(savedOrg);
        owner.setOrganizationRole(OrganizationRole.OWNER);
        userRepository.save(owner);
        savedOrg.setOwner(owner);
        savedOrg = organizationRepository.save(savedOrg);
        Organization finalSavedOrg = savedOrg;
        logService.log(builder -> builder
                .eventType("ORGANIZATION_CREATED")
                .description("Organización creada: " + finalSavedOrg.getName())
                .user(owner)
                .details("Plan: " + finalSavedOrg.getPlan() + ", Slug: " + finalSavedOrg.getSlug())
        );
        log.info("Organization registered successfully: {}", savedOrg.getName());
        return savedOrg;
    }
    @Override
    @Transactional
    public Organization createOrganization(Organization organization) {
        log.debug("Creating organization: {}", organization.getName());
        if (organizationRepository.existsBySlug(organization.getSlug())) {
            throw new IllegalArgumentException("El identificador (slug) ya está en uso");
        }
        applyPlanLimitsFromConfig(organization);
        Organization savedOrg = organizationRepository.save(organization);
        log.info("Organization created: {}", savedOrg.getId());
        return savedOrg;
    }
    @Override
    @Transactional
    public Organization updateOrganization(Long id, Organization organization) {
        log.debug("Updating organization: {}", id);
        Organization existingOrg = getOrganizationById(id);
        existingOrg.setName(organization.getName());
        existingOrg.setDescription(organization.getDescription());
        existingOrg.setContactEmail(organization.getContactEmail());
        existingOrg.setPhone(organization.getPhone());
        existingOrg.setAddress(organization.getAddress());
        existingOrg.setLogoUrl(organization.getLogoUrl());
        Organization updatedOrg = organizationRepository.save(existingOrg);
        logService.log(builder -> builder
                .eventType("ORGANIZATION_UPDATED")
                .description("Organización actualizada: " + updatedOrg.getName())
        );
        log.info("Organization updated: {}", id);
        return updatedOrg;
    }
    @Override
    @Transactional
    public void deactivateOrganization(Long id) {
        log.debug("Deactivating organization: {}", id);
        Organization organization = getOrganizationById(id);
        organization.setActive(false);
        organizationRepository.save(organization);
        logService.log(builder -> builder
                .eventType("ORGANIZATION_DEACTIVATED")
                .description("Organización desactivada: " + organization.getName())
        );
        log.info("Organization deactivated: {}", id);
    }
    @Override
    @Transactional(readOnly = true)
    public Organization getOrganizationById(Long id) {
        return organizationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Organización no encontrada"));
    }
    @Override
    @Transactional(readOnly = true)
    public Optional<Organization> findById(Long id) {
        return organizationRepository.findById(id);
    }
    @Override
    @Transactional(readOnly = true)
    public Organization getOrganizationBySlug(String slug) {
        return organizationRepository.findBySlug(slug)
                .orElseThrow(() -> new IllegalArgumentException("Organización no encontrada"));
    }
    @Override
    @Transactional(readOnly = true)
    public List<Organization> listOrganizations() {
        return organizationRepository.findAll();
    }
    @Override
    @Transactional(readOnly = true)
    public List<Organization> findAll() {
        return listOrganizations();
    }
    @Override
    @Transactional(readOnly = true)
    public List<Organization> listActiveOrganizations() {
        return organizationRepository.findByActiveTrue();
    }
    @Override
    @Transactional(readOnly = true)
    public boolean isSlugAvailable(String slug) {
        return !organizationRepository.existsBySlug(slug);
    }
    @Override
    @Transactional(readOnly = true)
    public boolean canAddUser(Long organizationId) {
        Organization org = getOrganizationById(organizationId);
        long currentUsers = organizationRepository.countUsersByOrganization(org);
        if (org.getPlan() == OrganizationPlan.ENTERPRISE) {
            return true;
        }
        return currentUsers < org.getMaxUsers();
    }
    @Override
    @Transactional(readOnly = true)
    public boolean canAddActivity(Long organizationId) {
        Organization org = getOrganizationById(organizationId);
        long currentActivities = organizationRepository.countActivitiesByOrganization(org);
        if (org.getPlan() == OrganizationPlan.ENTERPRISE) {
            return true;
        }
        return currentActivities < org.getMaxActivities();
    }
    @Override
    @Transactional(readOnly = true)
    public long getUserCount(Long organizationId) {
        Organization org = getOrganizationById(organizationId);
        return organizationRepository.countUsersByOrganization(org);
    }
    @Override
    @Transactional(readOnly = true)
    public long getActivityCount(Long organizationId) {
        Organization org = getOrganizationById(organizationId);
        return organizationRepository.countActivitiesByOrganization(org);
    }
    @Override
    @Transactional
    public void addUserToOrganization(Long userId, Long organizationId) {
        log.debug("Adding user {} to organization {}", userId, organizationId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        Organization organization = getOrganizationById(organizationId);
        if (!canAddUser(organizationId)) {
            throw new IllegalStateException("La organización ha alcanzado el límite de usuarios para su plan");
        }
        user.setOrganization(organization);
        userRepository.save(user);
        logService.log(builder -> builder
                .eventType("USER_ADDED_TO_ORG")
                .description("Usuario agregado a organización: " + organization.getName())
                .user(user)
        );
        log.info("User {} added to organization {}", userId, organizationId);
    }
    @Override
    @Transactional
    public void removeUserFromOrganization(Long userId) {
        log.debug("Removing user {} from organization", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        user.setOrganization(null);
        user.setOrganizationRole(null);
        userRepository.save(user);
        logService.log(builder -> builder
                .eventType("USER_REMOVED_FROM_ORG")
                .description("Usuario removido de organización")
                .user(user)
        );
        log.info("User {} removed from organization", userId);
    }
    @Override
    @Transactional
    public Organization changePlan(Long organizationId, OrganizationPlan newPlan) {
        log.debug("Changing plan for organization {} to {}", organizationId, newPlan);
        Organization organization = getOrganizationById(organizationId);
        OrganizationPlan oldPlan = organization.getPlan();
        organization.setPlan(newPlan);
        applyPlanLimitsFromConfig(organization);
        Organization updatedOrg = organizationRepository.save(organization);
        logService.log(builder -> builder
                .eventType("ORGANIZATION_PLAN_CHANGED")
                .description("Plan cambiado para organización: " + organization.getName())
                .details("Plan anterior: " + oldPlan + ", Plan nuevo: " + newPlan)
        );
        log.info("Plan changed for organization {}: {} -> {}", organizationId, oldPlan, newPlan);
        return updatedOrg;
    }
    private void applyPlanLimitsFromConfig(Organization organization) {
        organization.applyPlanLimits(
                planProperties.getFree().getMaxUsers(),
                planProperties.getFree().getMaxActivities(),
                planProperties.getBasic().getMaxUsers(),
                planProperties.getBasic().getMaxActivities(),
                planProperties.getPremium().getMaxUsers(),
                planProperties.getPremium().getMaxActivities()
        );
    }
    @Override
    @Transactional(readOnly = true)
    public OrganizationStatsDTO getStatsByOrganization(Long organizationId) {
        log.debug("Getting stats for organization: {}", organizationId);
        Organization organization = getOrganizationById(organizationId);
        long totalMembers = userRepository.countByOrganization(organization);
        long adminCount = userRepository.countByOrganizationAndOrganizationRole(organization, OrganizationRole.ADMIN);
        long memberCount = userRepository.countByOrganizationAndOrganizationRole(organization, OrganizationRole.MEMBER);
        List<Activity> activities = activityRepository.findByOrganization(organization);
        long activeActivities = activities.stream()
                .filter(a -> a.getStatus() == ActivityStatus.SCHEDULED)
                .count();
        long completedActivities = activities.stream()
                .filter(a -> a.getStatus() == ActivityStatus.COMPLETED)
                .count();
        long scheduledSessions = activities.stream()
                .flatMap(a -> sessionRepository.findByActivity(a).stream())
                .filter(s -> s.getStatus() == SessionStatus.PLANNED)
                .count();
        return OrganizationStatsDTO.builder()
                .totalMembers(totalMembers)
                .adminCount(adminCount)
                .memberCount(memberCount)
                .activeActivities(activeActivities)
                .completedActivities(completedActivities)
                .totalActivities(activities.size())
                .scheduledSessions(scheduledSessions)
                .build();
    }
}
