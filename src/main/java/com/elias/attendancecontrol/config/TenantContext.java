package com.elias.attendancecontrol.config;
public class TenantContext {
    private static final ThreadLocal<Long> currentOrganizationId = new ThreadLocal<>();
    public static void setCurrentOrganizationId(Long organizationId) {
        currentOrganizationId.set(organizationId);
    }
    public static Long getCurrentOrganizationId() {
        return currentOrganizationId.get();
    }
    public static void clear() {
        currentOrganizationId.remove();
    }
    public static boolean hasCurrentOrganization() {
        return currentOrganizationId.get() != null;
    }
}