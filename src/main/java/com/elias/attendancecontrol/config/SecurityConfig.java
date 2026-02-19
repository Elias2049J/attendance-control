package com.elias.attendancecontrol.config;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.XXssProtectionHeaderWriter;
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private static final String LOGIN_URL = "/auth/login";
    private static final String SYS_ROLE_ADMIN = "ADMIN";
    private static final String ORG_ROLE_OWNER = "ORG_OWNER";
    private static final String ORG_ROLE_ADMIN = "ORG_ADMIN";
    private final CustomAuthenticationProvider customAuthenticationProvider;
    private final TenantFilter tenantFilter;
    @Value("${security.max-sessions-per-user:1}")
    private int maxSessionsPerUser;
    @Value("${security.session-prevents-login:false}")
    private boolean maxSessionsPreventsLogin;
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
        http.getSharedObject(AuthenticationManagerBuilder.class)
                .authenticationProvider(customAuthenticationProvider);
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                        LOGIN_URL,
                        "/auth/register",
                        "/organizations/register",
                        "/css/**",
                        "/js/**",
                        "/bootstrap-icons-1.13.1/**",
                        "/static/**",
                        "/webjars/**",
                        "/error"
                ).permitAll()
                .requestMatchers("/organizations/{slug}/members").hasRole(SYS_ROLE_ADMIN)
                .requestMatchers("/organizations/manage", "/organizations/edit", "/organizations/settings")
                    .hasAnyRole(ORG_ROLE_OWNER, ORG_ROLE_ADMIN)
                .requestMatchers("/org/{orgSlug}/attendance/verify").authenticated()
                .requestMatchers("/", "/home").authenticated()
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage(LOGIN_URL)
                .loginProcessingUrl(LOGIN_URL)
                .usernameParameter("username")
                .passwordParameter("password")
                .defaultSuccessUrl("/", true)
                .failureUrl(LOGIN_URL+"?error=true")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/auth/logout")
                .logoutSuccessUrl(LOGIN_URL+"?logout=true")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            .sessionManagement(session -> session
                .sessionFixation().migrateSession()
                .maximumSessions(maxSessionsPerUser)
                .maxSessionsPreventsLogin(maxSessionsPreventsLogin)
            )
            .headers(headers -> headers
                .frameOptions(HeadersConfigurer.FrameOptionsConfig::deny)
                .xssProtection(xss -> xss.headerValue(XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK))
                .contentSecurityPolicy(csp -> csp
                    .policyDirectives("default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'")
                )
            )
            .addFilterAfter(tenantFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
