package com.elias.attendancecontrol.service;
import com.elias.attendancecontrol.model.entity.Session;
import com.elias.attendancecontrol.model.entity.SessionStatus;
import java.util.List;
public interface SessionService {
    Session activateSession(Long id);
    Session closeSession(Long id);
    List<Session> listSessions();
    Session getSessionById(Long id);
    boolean validateState(Session session, SessionStatus targetStatus);
    boolean canActivate(Long sessionId);
    boolean canClose(Long sessionId);
    List<Session> generateSessions(Long activityId);
    List<Session> applyExceptions(Long activityId, List<Session> sessions);
    List<Session> getSessionsByActivity(Long activityId);
}
