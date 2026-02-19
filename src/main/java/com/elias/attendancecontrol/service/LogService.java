package com.elias.attendancecontrol.service;
import com.elias.attendancecontrol.model.entity.AuditLog;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Consumer;
public interface LogService {
    AuditLog log(Consumer<AuditLog.AuditLogBuilder> builderConsumer);
    List<AuditLog> listAuditLogs();
    List<AuditLog> searchLogs(Long userId, String eventType, LocalDateTime startDate, LocalDateTime endDate);
    List<AuditLog> filterAuditLogs(Long userId, String eventType, LocalDateTime startDate, LocalDateTime endDate);
    List<AuditLog> getLogsByUser(Long userId);
}
