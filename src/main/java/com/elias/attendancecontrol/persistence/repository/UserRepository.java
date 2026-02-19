package com.elias.attendancecontrol.persistence.repository;
import com.elias.attendancecontrol.model.entity.Organization;
import com.elias.attendancecontrol.model.entity.OrganizationRole;
import com.elias.attendancecontrol.model.entity.SystemRole;
import com.elias.attendancecontrol.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    List<User> findByOrganization(Organization organization);
    List<User> findByOrganizationAndActiveTrue(Organization organization);
    @Query("SELECT u FROM User u WHERE u.organization.id = :orgId")
    List<User> findByOrganizationId(@Param("orgId") Long organizationId);
    @Query("SELECT u FROM User u WHERE u.organization.id = :orgId AND u.active = true")
    List<User> findActiveByOrganizationId(@Param("orgId") Long organizationId);
    @Query("SELECT u FROM User u WHERE u.systemRole = :systemRole AND u.active = true AND u.id NOT IN :excludedIds")
    List<User> findActiveBySystemRoleAndIdNotIn(@Param("systemRole") SystemRole systemRole, @Param("excludedIds") List<Long> excludedIds);
    List<User> findBySystemRoleAndActiveTrue(SystemRole systemRole);
    @Query("SELECT u FROM User u WHERE " +
           "(LOWER(u.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(u.lastname) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(u.username) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%'))) AND " +
           "u.organization.id = :orgId")
    List<User> searchByMultipleFieldsAndOrganization(@Param("query") String query, @Param("orgId") Long organizationId);
    @Query("SELECT u FROM User u WHERE " +
           "(LOWER(u.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(u.lastname) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(u.username) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%'))) AND " +
           "u.organization.id = :orgId AND " +
           "u.systemRole = :role")
    List<User> searchByMultipleFieldsAndOrganizationAndRole(@Param("query") String query, @Param("orgId") Long organizationId, @Param("role") SystemRole role);
    @Query("SELECT u FROM User u WHERE " +
           "(LOWER(u.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(u.lastname) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(u.username) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%'))) AND " +
           "u.organization.id = :orgId AND " +
           "u.active = :active")
    List<User> searchByMultipleFieldsAndOrganizationAndActive(@Param("query") String query, @Param("orgId") Long organizationId, @Param("active") Boolean active);
    @Query("SELECT u FROM User u WHERE " +
           "(LOWER(u.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(u.lastname) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(u.username) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%'))) AND " +
           "u.organization.id = :orgId AND " +
           "u.systemRole = :role AND " +
           "u.active = :active")
    List<User> searchByAllCriteria(@Param("query") String query, @Param("orgId") Long organizationId, @Param("role") SystemRole role, @Param("active") Boolean active);
    long countByOrganization(Organization organization);
    long countByOrganizationAndOrganizationRole(Organization organization, OrganizationRole organizationRole);
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.organization WHERE u.username = :username")
    Optional<User> findByUsernameWithOrganization(@Param("username") String username);
}
