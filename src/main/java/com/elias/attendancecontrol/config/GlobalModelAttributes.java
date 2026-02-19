package com.elias.attendancecontrol.config;
import com.elias.attendancecontrol.model.entity.SystemRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
@Slf4j
@ControllerAdvice
@RequiredArgsConstructor
public class GlobalModelAttributes {
    private final SecurityUtils securityUtils;
    @ModelAttribute("isSysAdmin")
    public boolean isSysAdmin() {
        return securityUtils.getCurrentUser()
                .map(user -> user.getSystemRole() == SystemRole.ADMIN)
                .orElse(false);
    }
    @ModelAttribute("currentUsername")
    public String currentUsername() {
        return securityUtils.getCurrentUser()
                .map(user -> user.getUsername())
                .orElse(null);
    }
    @ModelAttribute("currentUserFullName")
    public String currentUserFullName() {
        return securityUtils.getCurrentUser()
                .map(user -> user.getName() + " " + user.getLastname())
                .orElse(null);
    }
}
