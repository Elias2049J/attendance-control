package com.elias.attendancecontrol.model.dto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationStatsDTO {
    private long totalMembers;
    private long adminCount;
    private long memberCount;
    private long activeActivities;
    private long completedActivities;
    private long totalActivities;
    private long scheduledSessions;
}
