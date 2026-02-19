package com.elias.attendancecontrol.service.implementation;
import com.elias.attendancecontrol.model.entity.*;
import com.elias.attendancecontrol.persistence.repository.QrTokenRepository;
import com.elias.attendancecontrol.persistence.repository.SessionRepository;
import com.elias.attendancecontrol.persistence.repository.SessionTokenRepository;
import com.elias.attendancecontrol.persistence.repository.UserRepository;
import com.elias.attendancecontrol.persistence.repository.AttendanceRepository;
import com.elias.attendancecontrol.service.TokenService;
import com.elias.attendancecontrol.service.QRGeneratorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenServiceImpl implements TokenService {
    private final SessionTokenRepository sessionTokenRepository;
    private final UserRepository userRepository;
    private final QrTokenRepository qrTokenRepository;
    private final SessionRepository sessionRepository;
    private final AttendanceRepository attendanceRepository;
    private final QRGeneratorService qrGeneratorService;
    @Value("${qr.duration-minutes}")
    private int qrDurationMinutes;
    @Override
    @Transactional(readOnly = true)
    public boolean validateSessionToken(String token) {
        Optional<SessionToken> sessionTokenOpt = sessionTokenRepository.findByTokenAndActiveTrue(token);
        if (sessionTokenOpt.isEmpty()) {
            return false;
        }
        SessionToken sessionToken = sessionTokenOpt.get();
        LocalDateTime now = LocalDateTime.now();
        return sessionToken.getActive()
            && now.isBefore(sessionToken.getExpirationTime())
            && sessionToken.getUser().getActive();
    }
    @Override
    @Transactional
    public void revokeToken(String token) {
        sessionTokenRepository.findByTokenAndActiveTrue(token)
            .ifPresent(sessionToken -> {
                sessionToken.setActive(false);
                sessionTokenRepository.save(sessionToken);
                log.info("Token revoked: {}", token);
            });
    }
    @Override
    @Transactional
    public QRToken generateQRToken(Long sessionId, int validityMinutes) {
        return generateQR(sessionId);
    }
    @Override
    public String encodeToken(String rawToken) {
        return rawToken;
    }
    @Override
    @Transactional
    public QRToken generateQR(Long sessionId) {
        log.debug("Generating QR for session: {}", sessionId);
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Sesión no encontrada"));
        if (session.getStatus() != SessionStatus.ACTIVE) {
            throw new IllegalStateException("Solo se puede generar QR para sesiones activas");
        }
        qrTokenRepository.findBySessionAndActiveTrue(session)
            .ifPresent(oldQr -> {
                oldQr.setActive(false);
                qrTokenRepository.save(oldQr);
                log.debug("Previous QR invalidated for session: {}", sessionId);
            });
        LocalDateTime now = LocalDateTime.now();
        int validityMinutes = qrDurationMinutes;
        QRToken qrToken = new QRToken();
        qrToken.setToken(UUID.randomUUID().toString());
        qrToken.setSession(session);
        qrToken.setValidFrom(now);
        qrToken.setValidUntil(now.plusMinutes(validityMinutes));
        qrToken.setExpirationTime(now.plusMinutes(validityMinutes));
        qrToken.setActive(true);
        QRToken savedToken = qrTokenRepository.save(qrToken);
        log.info("QR generated for session {}: {}", sessionId, savedToken.getToken());
        return savedToken;
    }
    @Override
    @Transactional
    public QRToken regenerateQR(Long sessionId) {
        log.debug("Regenerating QR for session: {}", sessionId);
        return generateQR(sessionId);
    }
    @Override
    @Transactional(readOnly = true)
    public boolean validateQR(String token) {
        Optional<QRToken> qrTokenOpt = qrTokenRepository.findByToken(token);
        if (qrTokenOpt.isEmpty()) {
            log.debug("QR token not found: {}", token);
            return false;
        }
        QRToken qrToken = qrTokenOpt.get();
        LocalDateTime now = LocalDateTime.now();
        boolean isValid = qrToken.getActive()
            && now.isAfter(qrToken.getValidFrom())
            && now.isBefore(qrToken.getValidUntil())
            && qrToken.getSession().getStatus() == SessionStatus.ACTIVE;
        log.debug("QR validation result for token {}: {}", token, isValid);
        return isValid;
    }
    @Override
    @Transactional
    public void invalidateQR(String token) {
        qrTokenRepository.findByToken(token)
            .ifPresent(qrToken -> {
                qrToken.setActive(false);
                qrTokenRepository.save(qrToken);
                log.info("QR token invalidated: {}", token);
            });
    }
    @Override
    @Transactional
    public void setExpiration(QRToken token, int minutes) {
        if (token == null) {
            throw new IllegalArgumentException("Token no puede ser nulo");
        }
        LocalDateTime newExpiration = LocalDateTime.now().plusMinutes(minutes);
        token.setExpirationTime(newExpiration);
        token.setValidUntil(newExpiration);
        qrTokenRepository.save(token);
        log.debug("QR token expiration updated to: {}", newExpiration);
    }
    @Override
    @Transactional(readOnly = true)
    public boolean checkExpiration(String token) {
        return qrTokenRepository.findByToken(token)
            .map(qrToken -> {
                LocalDateTime now = LocalDateTime.now();
                return now.isAfter(qrToken.getExpirationTime());
            })
            .orElse(true);
    }
    @Override
    @Transactional
    public void renewExpiration(String token, int additionalMinutes) {
        qrTokenRepository.findByToken(token)
            .ifPresent(qrToken -> {
                if (qrToken.getActive()) {
                    LocalDateTime newExpiration = qrToken.getExpirationTime().plusMinutes(additionalMinutes);
                    qrToken.setExpirationTime(newExpiration);
                    qrToken.setValidUntil(newExpiration);
                    qrTokenRepository.save(qrToken);
                    log.info("QR token expiration renewed: {} minutes added", additionalMinutes);
                }
            });
    }
    @Override
    @Transactional
    public void invalidateToken(String token) {
        revokeToken(token); // Intenta como SessionToken
        invalidateQR(token); // Intenta como QRToken
    }
    @Override
    @Transactional
    public void invalidateExpiredTokens() {
        log.debug("Invalidating expired tokens");
        LocalDateTime now = LocalDateTime.now();
        List<SessionToken> expiredSessionTokens = sessionTokenRepository
            .findAll()
            .stream()
            .filter(t -> t.getActive() && t.getExpirationTime().isBefore(now))
            .toList();
        expiredSessionTokens.forEach(t -> {
            t.setActive(false);
            sessionTokenRepository.save(t);
        });
        List<QRToken> expiredQRTokens = qrTokenRepository
            .findAll()
            .stream()
            .filter(t -> t.getActive() && t.getExpirationTime().isBefore(now))
            .toList();
        expiredQRTokens.forEach(t -> {
            t.setActive(false);
            qrTokenRepository.save(t);
        });
        log.info("Expired tokens invalidated: {} session tokens, {} QR tokens",
            expiredSessionTokens.size(), expiredQRTokens.size());
    }
    @Override
    @Transactional
    public void invalidateUserSessionTokens(String username) {
        log.debug("Invalidating all session tokens for user: {}", username);
        List<SessionToken> activeTokens = sessionTokenRepository.findByUser_UsernameAndActiveTrue(username);
        for (SessionToken token : activeTokens) {
            token.setActive(false);
            sessionTokenRepository.save(token);
        }
        log.info("Invalidated {} session token(s) for user: {}", activeTokens.size(), username);
    }
    @Override
    @Transactional
    public void invalidateUserSessionTokensById(Long userId) {
        log.debug("Invalidating all session tokens for user ID: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        List<SessionToken> activeTokens = sessionTokenRepository.findByUserAndActiveTrue(user);
        for (SessionToken token : activeTokens) {
            token.setActive(false);
            sessionTokenRepository.save(token);
        }
        log.info("Invalidated {} session token(s) for user ID: {}", activeTokens.size(), userId);
    }
    @Override
    @Transactional
    public Map<String, Object> generateQRWithFullData(Long sessionId, String baseUrl) {
        log.debug("Generating QR with full data for session: {}", sessionId);
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Sesión no encontrada"));
        QRToken qrToken = generateQR(sessionId);
        String orgSlug = session.getActivity().getOrganization().getSlug();
        String verificationLink = baseUrl + "/org/" + orgSlug + "/attendance/verify?token=" + qrToken.getToken();
        String qrImageBase64 = qrGeneratorService.generateAttendanceQR(verificationLink);
        List<Attendance> attendances = attendanceRepository.findBySession(session);
        Map<String, Object> data = new HashMap<>();
        data.put("qrToken", qrToken);
        data.put("session", session);
        data.put("sessionId", sessionId);
        data.put("activityName", session.getActivity().getName());
        data.put("sessionDate", session.getSessionDate());
        data.put("sessionTime", session.getStartTime() + " - " + session.getEndTime());
        data.put("verificationLink", verificationLink);
        data.put("qrImage", qrImageBase64);
        data.put("attendances", attendances);
        data.put("organizationSlug", orgSlug);
        log.info("QR with full data generated for session {} with organization slug: {}", sessionId, orgSlug);
        return data;
    }
    @Override
    @Transactional
    public void autoRegenerateActiveSessionQRs() {
        log.debug("Auto-regenerating QR codes for active sessions");
        List<Session> activeSessions = sessionRepository.findByStatus(SessionStatus.ACTIVE);
        int regeneratedCount = 0;
        for (Session session : activeSessions) {
            try {
                regenerateQR(session.getId());
                regeneratedCount++;
            } catch (Exception e) {
                log.error("Error auto-regenerating QR for session {}: {}", session.getId(), e.getMessage());
            }
        }
        log.info("Auto-regenerated {} QR code(s) for active sessions", regeneratedCount);
    }
}
