package com.elias.attendancecontrol.service;
import com.elias.attendancecontrol.model.entity.QRToken;
import java.util.Map;
public interface TokenService {
    QRToken getQRTokenByToken(String token);
    boolean validateSessionToken(String token);
    QRToken regenerateQR(Long sessionId);
    boolean validateQR(String token);
    QRToken getQRTokenWithSessionAndOrganization(String token, String orgSlug);
    void invalidateUserSessionTokens(String username);
    void invalidateUserSessionTokensById(Long userId);
    Map<String, Object> generateQRWithFullData(Long sessionId, String baseUrl);
    void autoRegenerateActiveSessionQRs();
}
