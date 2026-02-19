package com.elias.attendancecontrol.service.implementation;
import com.elias.attendancecontrol.config.SecurityUtils;
import com.elias.attendancecontrol.config.TenantContext;
import com.elias.attendancecontrol.model.entity.AuditLog;
import com.elias.attendancecontrol.model.entity.User;
import com.elias.attendancecontrol.persistence.repository.AuditLogRepository;
import com.elias.attendancecontrol.persistence.repository.UserRepository;
import com.elias.attendancecontrol.service.LogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Consumer;
@Slf4j
@Service
@RequiredArgsConstructor
public class LogServiceImpl implements LogService {
    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final SecurityUtils securityUtils;
    @Override
    @Transactional
    public AuditLog log(Consumer<AuditLog.AuditLogBuilder> builderConsumer) {
        AuditLog.AuditLogBuilder builder = AuditLog.builder()
                .eventDate(LocalDateTime.now());
        builderConsumer.accept(builder);
        AuditLog auditLog = builder.build();
        if (auditLog.getEventType() == null || auditLog.getEventType().trim().isEmpty()) {
            throw new IllegalArgumentException("El tipo de evento es obligatorio");
        }
        if (auditLog.getDescription() == null || auditLog.getDescription().trim().isEmpty()) {
            throw new IllegalArgumentException("La descripción es obligatoria");
        }
        if (auditLog.getUser() != null) {
            log.debug("Logging event: {} for user: {}", auditLog.getEventType(), auditLog.getUser().getUsername());
        } else {
            log.debug("Logging event: {} (no user)", auditLog.getEventType());
        }
        return auditLogRepository.save(auditLog);
    }
    @Override
    @Transactional(readOnly = true)
    public List<AuditLog> listAuditLogs() {
        if (securityUtils.isSystemAdmin()) {
            return auditLogRepository.findAll();
        }
        if (!TenantContext.hasCurrentOrganization()) {
            return List.of();
        }
        Long orgId = TenantContext.getCurrentOrganizationId();
        return auditLogRepository.findByOrganization(orgId);
    }
    @Override
    @Transactional(readOnly = true)
    public List<AuditLog> searchLogs(Long userId, String eventType, LocalDateTime startDate, LocalDateTime endDate) {
        log.debug("Searching logs: userId={}, eventType={}, startDate={}, endDate={}",
                userId, eventType, startDate, endDate);
        if (securityUtils.isSystemAdmin()) {
            return auditLogRepository.searchLogs(userId, eventType, startDate, endDate);
        }
        if (!TenantContext.hasCurrentOrganization()) {
            return List.of();
        }
        Long orgId = TenantContext.getCurrentOrganizationId();
        return auditLogRepository.searchLogsByOrganization(orgId, userId, eventType, startDate, endDate);
    }
    @Override
    @Transactional(readOnly = true)
    public List<AuditLog> filterAuditLogs(Long userId, String eventType, LocalDateTime startDate, LocalDateTime endDate) {
        return searchLogs(userId, eventType, startDate, endDate);
    }
    @Override
    @Transactional(readOnly = true)
    public List<AuditLog> getLogsByUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        return auditLogRepository.findByUser(user);
    }
}
