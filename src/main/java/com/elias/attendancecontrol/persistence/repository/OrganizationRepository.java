package com.elias.attendancecontrol.persistence.repository;
import com.elias.attendancecontrol.model.entity.Organization;
import com.elias.attendancecontrol.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
@Repository
public interface OrganizationRepository extends JpaRepository<Organization, Long> {
    Optional<Organization> findBySlug(String slug);
    boolean existsBySlug(String slug);
    List<Organization> findByActiveTrue();
    List<Organization> findByOwner(User owner);
    @Query("SELECT COUNT(u) FROM User u WHERE u.organization = :org")
    long countUsersByOrganization(@Param("org") Organization organization);
    @Query("SELECT COUNT(a) FROM Activity a WHERE a.organization = :org")
    long countActivitiesByOrganization(@Param("org") Organization organization);
    List<Organization> findByPlan(com.elias.attendancecontrol.model.entity.OrganizationPlan plan);
}
