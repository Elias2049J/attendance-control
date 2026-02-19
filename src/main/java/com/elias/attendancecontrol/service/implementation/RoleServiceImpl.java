package com.elias.attendancecontrol.service.implementation;
import com.elias.attendancecontrol.model.entity.SystemRole;
import com.elias.attendancecontrol.model.entity.User;
import com.elias.attendancecontrol.service.RoleService;
import org.springframework.stereotype.Service;
@Service
public class RoleServiceImpl implements RoleService {
    @Override
    public void assignRole(Long userId, SystemRole systemRole) {
    }
    @Override
    public boolean validateRole(User user, SystemRole systemRole) {
        return false;
    }
}
