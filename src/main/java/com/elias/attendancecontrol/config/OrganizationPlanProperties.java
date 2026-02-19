package com.elias.attendancecontrol.config;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "organization.plan")
public class OrganizationPlanProperties {
    private PlanLimits free = new PlanLimits();
    private PlanLimits basic = new PlanLimits();
    private PlanLimits premium = new PlanLimits();
    @Getter
    @Setter
    public static class PlanLimits {
        private int maxUsers;
        private int maxActivities;
    }
}
