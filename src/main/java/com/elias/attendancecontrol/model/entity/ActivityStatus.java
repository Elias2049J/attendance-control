package com.elias.attendancecontrol.model.entity;
public enum ActivityStatus {
    DRAFT("Borrador"),
    SCHEDULED("Programada"),
    PAUSED("Pausada"),
    COMPLETED("Completada"),
    CANCELLED("Cancelada");
    private final String displayName;
    ActivityStatus(String displayName) {
        this.displayName = displayName;
    }
    public String getDisplayName() {
        return displayName;
    }
    public boolean isEditable() {
        return this == DRAFT || this == SCHEDULED || this == PAUSED;
    }
    public boolean isActive() {
        return this == SCHEDULED;
    }
    public boolean isFinalState() {
        return this == COMPLETED || this == CANCELLED;
    }
}