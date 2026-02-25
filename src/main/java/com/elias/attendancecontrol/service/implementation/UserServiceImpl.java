package com.elias.attendancecontrol.service.implementation;

import com.elias.attendancecontrol.config.SecurityUtils;
import com.elias.attendancecontrol.model.entity.Organization;
import com.elias.attendancecontrol.model.entity.SystemRole;
import com.elias.attendancecontrol.model.entity.User;
import com.elias.attendancecontrol.persistence.repository.OrganizationRepository;
import com.elias.attendancecontrol.persistence.repository.UserRepository;
import com.elias.attendancecontrol.service.LogService;
import com.elias.attendancecontrol.service.OrganizationService;
import com.elias.attendancecontrol.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final PasswordEncoder passwordEncoder;
    private final LogService logService;
    private final OrganizationService organizationService;
    private final SecurityUtils securityUtils;
    @Override
    @Transactional
    public User registerUser(User user) {
        log.debug("Registering new user: {}", user.getUsername());
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new IllegalArgumentException("El nombre de usuario ya existe");
        }
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new IllegalArgumentException("El correo electrónico ya existe");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        if (user.getSystemRole() == null) {
            user.setSystemRole(SystemRole.USER);
        }
        if (user.getActive() == null) {
            user.setActive(true);
        }
        User savedUser = userRepository.save(user);
        log.info("User registered successfully: {}", savedUser.getUsername());
        return savedUser;
    }

    @Override
    @Transactional
    public User createUser(User user) {
        log.debug("Creating new user: {}", user.getUsername());
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new IllegalArgumentException("El nombre de usuario ya existe");
        }
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new IllegalArgumentException("El correo electrónico ya existe");
        }

        Organization organization = securityUtils.getCurrentOrganization().orElseThrow();

        if (!organizationService.canAddUser(organization.getId())) {
            throw new IllegalStateException(
                    "Has alcanzado el límite de usuarios de tu plan (" +
                            organization.getMaxUsers() + " usuarios)");
        }
        user.setOrganization(organization);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        User savedUser = userRepository.save(user);

        logService.log(builder -> builder
            .eventType("USER_CREATED")
            .description("Usuario creado: " + savedUser.getUsername())
            .user(savedUser)
            .details("SystemRole: " + savedUser.getSystemRole())
        );
        log.info("User created successfully: {}", savedUser.getUsername());
        return savedUser;
    }

    @Override
    @Transactional
    public User updateUser(Long userToUpdateId, User userToUpdate) {
        log.debug("Updating user: {}", userToUpdateId);
        User existingUser = userRepository.findById(userToUpdateId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        existingUser.setName(userToUpdate.getName());
        existingUser.setLastname(userToUpdate.getLastname());
        existingUser.setEmail(userToUpdate.getEmail());
        existingUser.setSystemRole(userToUpdate.getSystemRole());
        if (userToUpdate.getOrganizationRole().isOwner() &&
                !existingUser.getOrganizationRole().isOwner()) {
            userToUpdate.setOrganizationRole(existingUser.getOrganizationRole());
        }
        existingUser.setActive(userToUpdate.getActive());
        if (userToUpdate.getPassword() != null && !userToUpdate.getPassword().isEmpty()) {
            existingUser.setPassword(passwordEncoder.encode(userToUpdate.getPassword()));
        }
        User updatedUser = userRepository.save(existingUser);
        logService.log(builder -> builder
            .eventType("USER_UPDATED")
            .description("Usuario actualizado: " + updatedUser.getUsername())
            .user(updatedUser)
        );
        log.info("User updated successfully: {}", updatedUser.getUsername());
        return updatedUser;
    }
    @Override
    @Transactional
    public void deactivateUser(Long id) {
        log.debug("Deactivating user: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        user.setActive(false);
        userRepository.save(user);
        logService.log(builder -> builder
            .eventType("USER_DEACTIVATED")
            .description("Usuario desactivado: " + user.getUsername())
            .user(user)
        );
        log.info("User deactivated successfully: {}", user.getUsername());
    }
    @Override
    @Transactional(readOnly = true)
    public List<User> listUsers() {
        return securityUtils.getCurrentOrganizationId()
                .map(userRepository::findByOrganizationId)
                .orElseGet(userRepository::findAll);
    }
    @Override
    @Transactional(readOnly = true)
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
    }
    @Override
    @Transactional(readOnly = true)
    public List<User> getAvailableUsersExcluding(SystemRole systemRole, List<Long> excludedUserIds) {
        if (excludedUserIds == null || excludedUserIds.isEmpty()) {
            return userRepository.findBySystemRoleAndActiveTrue(systemRole);
        }
        return userRepository.findActiveBySystemRoleAndIdNotIn(systemRole, excludedUserIds);
    }
    @Override
    @Transactional(readOnly = true)
    public List<User> getActiveUsersBySystemRole(SystemRole systemRole) {
        return userRepository.findBySystemRoleAndActiveTrue(systemRole);
    }
    @Override
    @Transactional(readOnly = true)
    public List<User> findByOrganizationId(Long organizationId) {
        return userRepository.findByOrganizationId(organizationId);
    }
    @Override
    @Transactional(readOnly = true)
    public List<User> searchUsers(String query, SystemRole role, Boolean active) {
        log.debug("Searching users: query={}, role={}, active={}", query, role, active);

        if (query == null || query.trim().isEmpty()) {
            return listUsers();
        }

        return securityUtils.getCurrentOrganizationId()
                .map(orgId -> {
                    if (role != null && active != null) {
                        return userRepository.searchByAllCriteria(query, orgId, role, active);
                    } else if (role != null) {
                        return userRepository.searchByMultipleFieldsAndOrganizationAndRole(query, orgId, role);
                    } else if (active != null) {
                        return userRepository.searchByMultipleFieldsAndOrganizationAndActive(query, orgId, active);
                    } else {
                        return userRepository.searchByMultipleFieldsAndOrganization(query, orgId);
                    }
                })
                .orElse(Collections.emptyList());
    }
}
