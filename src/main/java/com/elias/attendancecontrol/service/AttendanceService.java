package com.elias.attendancecontrol.service;

import com.elias.attendancecontrol.model.entity.Attendance;
import com.elias.attendancecontrol.model.entity.AttendanceStatus;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

public interface AttendanceService {

    Attendance registerAttendance(Long userId, String qrToken);

    List<Attendance> manualRegistrationBatch(Long sessionId, Map<Long, AttendanceStatus> userAttendances);

    List<Attendance> listAttendance();

    @Transactional(readOnly = true)
    boolean validateSession(Long sessionId);

    @Transactional(readOnly = true)
    boolean validateUser(Long userId);

    @Transactional(readOnly = true)
    boolean validateTime(Long sessionId);

    @Transactional(readOnly = true)
    boolean checkDuplicate(Long sessionId, Long userId);

    @Transactional(readOnly = true)
    List<Attendance> getAttendanceBySession(Long sessionId);

    @Transactional(readOnly = true)
    List<Attendance> getAttendanceByUser(Long userId);
}

