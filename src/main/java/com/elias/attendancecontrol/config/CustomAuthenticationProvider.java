package com.elias.attendancecontrol.config;
import com.elias.attendancecontrol.model.entity.User;
import com.elias.attendancecontrol.service.AuthenticationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
@Slf4j
@Component
@RequiredArgsConstructor
public class CustomAuthenticationProvider implements AuthenticationProvider {
    private final AuthenticationService authenticationService;
    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String username = authentication.getName();
        Object credentials = authentication.getCredentials();
        if (credentials == null) {
            log.warn("Authentication failed for user: {} - No credentials provided", username);
            throw new BadCredentialsException("Credenciales no proporcionadas");
        }
        String password = credentials.toString();
        log.debug("Attempting authentication for user: {}", username);
        try {
            User user = authenticationService.authenticate(username, password);
            var authorities = new ArrayList<SimpleGrantedAuthority>();
            authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getSystemRole().name()));
            if (user.getOrganizationRole() != null) {
                authorities.add(new SimpleGrantedAuthority("ROLE_ORG_" + user.getOrganizationRole().name()));
            }
            log.info("User authenticated successfully: {}, systemRole: {}, orgRole: {}",
                    username, user.getSystemRole(), user.getOrganizationRole());
            CustomUserDetails userDetails = new CustomUserDetails(user, authorities);
            return new UsernamePasswordAuthenticationToken(
                    userDetails,
                    null,
                    authorities
            );
        } catch (IllegalArgumentException e) {
            log.warn("Authentication failed for user: {} - Invalid credentials", username);
            throw new BadCredentialsException("Credenciales inválidas", e);
        } catch (IllegalStateException e) {
            log.warn("Authentication failed for user: {} - User inactive", username);
            throw new BadCredentialsException("Usuario inactivo", e);
        }
    }
    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
