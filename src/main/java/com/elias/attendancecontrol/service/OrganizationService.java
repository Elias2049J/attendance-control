package com.elias.attendancecontrol.service;
import com.elias.attendancecontrol.model.dto.OrganizationStatsDTO;
import com.elias.attendancecontrol.model.entity.Organization;
import com.elias.attendancecontrol.model.entity.User;
import java.util.List;
import java.util.Optional;
public interface OrganizationService {
    Organization registerOrganization(Organization organization, User owner);
    Organization createOrganization(Organization organization);
    Organization updateOrganization(Long id, Organization organization);
    void deactivateOrganization(Long id);
    Organization getOrganizationById(Long id);
    Optional<Organization> findById(Long id);
    Organization getOrganizationBySlug(String slug);
    List<Organization> listOrganizations();
    List<Organization> findAll();
    List<Organization> listActiveOrganizations();
    boolean isSlugAvailable(String slug);
    boolean canAddUser(Long organizationId);
    boolean canAddActivity(Long organizationId);
    long getUserCount(Long organizationId);
    long getActivityCount(Long organizationId);
    void addUserToOrganization(Long userId, Long organizationId);
    void removeUserFromOrganization(Long userId);
    Organization changePlan(Long organizationId, com.elias.attendancecontrol.model.entity.OrganizationPlan newPlan);
    OrganizationStatsDTO getStatsByOrganization(Long organizationId);
}
