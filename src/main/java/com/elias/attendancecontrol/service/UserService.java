package com.elias.attendancecontrol.service;
import com.elias.attendancecontrol.model.entity.SystemRole;
import com.elias.attendancecontrol.model.entity.User;
import java.util.List;
public interface UserService {
    User registerUser(User user);
    User createUser(User user);
    User updateUser(Long userToUpdateId, User userToUpdate);
    void deactivateUser(Long id);
    List<User> listUsers();
    User getUserById(Long id);
    List<User> getAvailableUsersExcluding(SystemRole systemRole, List<Long> excludedUserIds);
    List<User> getActiveUsersBySystemRole(SystemRole systemRole);
    List<User> findByOrganizationId(Long organizationId);
    List<User> searchUsers(String query, SystemRole role, Boolean active);
}
