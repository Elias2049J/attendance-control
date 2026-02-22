package com.elias.attendancecontrol.service.implementation;

import com.elias.attendancecontrol.config.SecurityUtils;
import com.elias.attendancecontrol.model.entity.Activity;
import com.elias.attendancecontrol.model.entity.Session;
import com.elias.attendancecontrol.persistence.repository.ActivityRepository;
import com.elias.attendancecontrol.persistence.repository.SessionRepository;
import com.elias.attendancecontrol.service.CalendarService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CalendarServiceImpl implements CalendarService {

    private final SessionRepository sessionRepository;
    private final ActivityRepository activityRepository;
    private final SecurityUtils securityUtils;

    @Override
    @Transactional(readOnly = true)
    public List<Session> getCalendarView(LocalDate startDate, LocalDate endDate) {
        return securityUtils.getCurrentOrganizationId()
                .map(orgId -> {
                    log.debug("Getting calendar view for organization: {} from {} to {}", orgId, startDate, endDate);
                    return sessionRepository.findBySessionDateBetweenAndActivityOrganizationId(startDate, endDate, orgId);
                })
                .orElseGet(() -> {
                    log.debug("No organization context, getting all sessions from {} to {}", startDate, endDate);
                    return sessionRepository.findBySessionDateBetween(startDate, endDate);
                });
    }

    @Override
    @Transactional(readOnly = true)
    public List<Activity> getActivitiesByDateRange(LocalDate startDate, LocalDate endDate) {
        return securityUtils.getCurrentOrganizationId()
                .map(orgId -> {
                    log.debug("Getting activities for organization: {} between {} and {}", orgId, startDate, endDate);
                    return activityRepository.findByOrganizationIdAndSessionsDateBetween(orgId, startDate, endDate);
                })
                .orElseGet(() -> {
                    log.debug("No organization context, getting all activities by date range from {} to {}", startDate, endDate);
                    return activityRepository.findBySessionsDateBetween(startDate, endDate);
                });
    }
}
