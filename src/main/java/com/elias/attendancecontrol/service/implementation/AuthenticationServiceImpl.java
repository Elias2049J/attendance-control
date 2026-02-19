package com.elias.attendancecontrol.service.implementation;
import com.elias.attendancecontrol.model.entity.SessionToken;
import com.elias.attendancecontrol.model.entity.User;
import com.elias.attendancecontrol.persistence.repository.SessionTokenRepository;
import com.elias.attendancecontrol.persistence.repository.UserRepository;
import com.elias.attendancecontrol.service.AuthenticationService;
import com.elias.attendancecontrol.service.LogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.UUID;
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {
    private final UserRepository userRepository;
    private final SessionTokenRepository sessionTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final LogService logService;
    @Value("${session-token.duration-hours}")
    private int sessionTokenDurationHours;
    @Override
    @Transactional(readOnly = true)
    public User authenticate(String username, String password) {
        log.debug("Attempting to authenticate user: {}", username);
        User user = userRepository.findByUsernameWithOrganization(username)
                .orElseThrow(() -> new IllegalArgumentException("Credenciales inválidas"));
        if (!user.getActive()) {
            log.warn("Inactive user attempted to login: {}", username);
            throw new IllegalStateException("Usuario inactivo");
        }
        if (!passwordEncoder.matches(password, user.getPassword())) {
            log.warn("Invalid password for user: {}", username);
            throw new IllegalArgumentException("Credenciales inválidas");
        }
        log.info("User authenticated successfully: {}", username);
        return user;
    }
    @Override
    @Transactional
    public String createSession(User user, String ipAddress) {
        log.debug("Creating session for user: {}", user.getUsername());
        String token = UUID.randomUUID().toString();
        LocalDateTime expirationTime = LocalDateTime.now().plusHours(sessionTokenDurationHours);
        SessionToken sessionToken = new SessionToken();
        sessionToken.setToken(token);
        sessionToken.setUser(user);
        sessionToken.setIpAddress(ipAddress);
        sessionToken.setExpirationTime(expirationTime);
        sessionToken.setActive(true);
        sessionTokenRepository.save(sessionToken);
        logService.log(builder -> builder
                .eventType("LOGIN")
                .description("Usuario autenticado exitosamente")
                .user(user)
                .ipAddress(ipAddress)
        );
        log.info("Session created for user: {}", user.getUsername());
        return token;
    }
}
