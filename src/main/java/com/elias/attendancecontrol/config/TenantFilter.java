package com.elias.attendancecontrol.config;
import com.elias.attendancecontrol.model.entity.SystemRole;
import jakarta.servlet.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import java.io.IOException;
@Slf4j
@Component
@RequiredArgsConstructor
public class TenantFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                Object principal = authentication.getPrincipal();
                if (principal instanceof CustomUserDetails userDetails) {
                    var user = userDetails.getUser();
                    if (user.getOrganization() != null) {
                        Long orgId = user.getOrganization().getId();
                        TenantContext.setCurrentOrganizationId(orgId);
                        log.debug("Tenant context set for user: {}, organization: {}, orgRole: {}",
                                user.getUsername(), orgId, user.getOrganizationRole());
                    } else {
                        log.debug("User {} has no organization (system role: {})",
                                user.getUsername(), user.getSystemRole());
                        if (user.getSystemRole() != SystemRole.ADMIN) {
                            log.warn("User {} has no organization but is not ADMIN - potential data issue",
                                    user.getUsername());
                        }
                    }
                }
            }
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        log.info("TenantFilter initialized");
    }
    @Override
    public void destroy() {
        log.info("TenantFilter destroyed");
    }
}
