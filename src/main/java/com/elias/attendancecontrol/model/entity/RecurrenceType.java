package com.elias.attendancecontrol.model.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum RecurrenceType {
    NONE("Ninguna"),
    DAILY("Diario"),
    WEEKLY("Semanal"),
    MONTHLY("Mensual");

    private final String displayName;
}
