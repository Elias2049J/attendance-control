package com.elias.attendancecontrol.service.implementation;
import com.elias.attendancecontrol.model.entity.*;
import com.elias.attendancecontrol.persistence.repository.QrTokenRepository;
import com.elias.attendancecontrol.persistence.repository.SessionRepository;
import com.elias.attendancecontrol.persistence.repository.SessionTokenRepository;
import com.elias.attendancecontrol.persistence.repository.UserRepository;
import com.elias.attendancecontrol.persistence.repository.AttendanceRepository;
import com.elias.attendancecontrol.service.TokenService;
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
    public QRToken getQRTokenByToken(String token) {
        return qrTokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Token QR no encontrado"));
    }

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

    private QRToken generateQR(Session session) {
        if (!session.getStatus().isActive()) {
            throw new IllegalStateException("Solo se puede generar QR para sesiones activas");
        }

        LocalDateTime now = LocalDateTime.now();
        int validityMinutes = qrDurationMinutes;

        Optional<QRToken> existingQrOpt = qrTokenRepository.findBySession(session);

        QRToken qrToken;
        if (existingQrOpt.isPresent()) {
            qrToken = existingQrOpt.get();
            qrToken.setToken(UUID.randomUUID().toString());
            qrToken.setValidFrom(now);
            qrToken.setValidUntil(now.plusMinutes(validityMinutes));
            qrToken.setExpirationTime(now.plusMinutes(validityMinutes));
            qrToken.setActive(true);
            log.debug("Updating existing QR for session: {}", session.getId());
        } else {
            qrToken = new QRToken();
            qrToken.setToken(UUID.randomUUID().toString());
            qrToken.setSession(session);
            qrToken.setValidFrom(now);
            qrToken.setValidUntil(now.plusMinutes(validityMinutes));
            qrToken.setExpirationTime(now.plusMinutes(validityMinutes));
            qrToken.setActive(true);
            log.debug("Creating new QR for session: {}", session.getId());
        }

        QRToken savedToken = qrTokenRepository.save(qrToken);
        log.info("QR generated for session {}: {}", session.getId(), savedToken.getToken());
        return savedToken;
    }

    @Override
    @Transactional
    public QRToken regenerateQR(Long sessionId) {
        log.debug("Regenerating QR for session: {}", sessionId);
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Sesión no encontrada"));
        return generateQR(session);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean validateQR(String token) {
        Optional<QRToken> qrTokenOpt = qrTokenRepository.findByTokenWithSessionAndOrganization(token);
        if (qrTokenOpt.isEmpty()) {
            log.debug("QR token not found: {}", token);
            return false;
        }
        QRToken qrToken = qrTokenOpt.get();
        LocalDateTime now = LocalDateTime.now();
        boolean isValid = qrToken.getActive()
            && now.isAfter(qrToken.getValidFrom())
            && now.isBefore(qrToken.getValidUntil())
            && qrToken.getSession().getStatus().isActive();
        log.debug("QR validation result for token {}: {}", token, isValid);
        return isValid;
    }

    @Override
    @Transactional(readOnly = true)
    public QRToken getQRTokenWithSessionAndOrganization(String token, String orgSlug) {
        log.debug("Getting QR token with session and organization validation for token: {} and org: {}", token, orgSlug);

        QRToken qrToken = qrTokenRepository.findByTokenWithSessionAndOrganization(token)
                .orElseThrow(() -> new IllegalArgumentException("Token QR no encontrado"));

        String sessionOrgSlug = qrToken.getSession().getActivity().getOrganization().getSlug();

        if (!sessionOrgSlug.equals(orgSlug)) {
            log.warn("Organization slug mismatch. Expected: {}, Got: {}", sessionOrgSlug, orgSlug);
            throw new IllegalArgumentException("Organización no válida para esta sesión");
        }

        return qrToken;
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

        Session session = sessionRepository.findByIdWithActivityAndOrganization(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Sesión no encontrada"));

        QRToken qrToken = generateQR(session);
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
