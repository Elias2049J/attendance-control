package com.elias.attendancecontrol.model.entity;
public enum RecurrenceType {
    NONE("Ninguna"),
    DAILY("Diario"),
    WEEKLY("Semanal"),
    MONTHLY("Mensual");
    private final String displayName;
    RecurrenceType(String displayName) {
        this.displayName = displayName;
    }
    public String getDisplayName() {
        return displayName;
    }
}
