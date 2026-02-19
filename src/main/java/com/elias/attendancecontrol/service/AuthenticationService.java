package com.elias.attendancecontrol.service;
import com.elias.attendancecontrol.model.entity.User;
public interface AuthenticationService {
    User authenticate(String username, String password);
    String createSession(User user, String ipAddress);
}
