package com.elias.attendancecontrol.service;
import com.elias.attendancecontrol.model.entity.QRToken;
import com.elias.attendancecontrol.model.entity.SessionToken;
import java.util.Map;
public interface TokenService {
    boolean validateSessionToken(String token);
    void revokeToken(String token);
    QRToken generateQRToken(Long sessionId, int validityMinutes);
    String encodeToken(String rawToken);
    QRToken generateQR(Long sessionId);
    QRToken regenerateQR(Long sessionId);
    boolean validateQR(String token);
    void invalidateQR(String token);
    void setExpiration(QRToken token, int minutes);
    boolean checkExpiration(String token);
    void renewExpiration(String token, int additionalMinutes);
    void invalidateToken(String token);
    void invalidateExpiredTokens();
    void invalidateUserSessionTokens(String username);
    void invalidateUserSessionTokensById(Long userId);
    Map<String, Object> generateQRWithFullData(Long sessionId, String baseUrl);
    void autoRegenerateActiveSessionQRs();
}
