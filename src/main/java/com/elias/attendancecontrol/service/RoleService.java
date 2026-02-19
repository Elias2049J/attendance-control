package com.elias.attendancecontrol.service;
import com.elias.attendancecontrol.model.entity.SystemRole;
import com.elias.attendancecontrol.model.entity.User;
public interface RoleService {
    void assignRole(Long userId, SystemRole systemRole);
    boolean validateRole(User user, SystemRole systemRole);
}
