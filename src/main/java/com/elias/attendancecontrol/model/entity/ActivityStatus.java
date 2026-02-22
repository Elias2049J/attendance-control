package com.elias.attendancecontrol.model.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ActivityStatus {
    DRAFT("Borrador"),
    SCHEDULED("Programada"),
    PAUSED("Pausada"),
    COMPLETED("Completada"),
    CANCELLED("Cancelada");
    private final String displayName;

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