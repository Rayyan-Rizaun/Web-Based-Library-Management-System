package com.lms.common.domain;

/**
 * {@code BookIncident.Status} — CK_BookIncident_Status.
 *
 * <p>Some stored values contain a space, which no Java constant name can,
 * so the column is mapped with {@link JpaConverter} rather than
 * {@code @Enumerated} — see {@link DbValueEnum}.
 */
public enum IncidentStatus implements DbValueEnum {

    Open("Open"),
    Charged("Charged"),
    Resolved("Resolved"),
    WrittenOff("Written Off");

    private final String dbValue;

    IncidentStatus(String dbValue) {
        this.dbValue = dbValue;
    }

    @Override
    public String dbValue() {
        return dbValue;
    }

    @jakarta.persistence.Converter
    public static class JpaConverter extends DbValueEnumConverter<IncidentStatus> {
        public JpaConverter() {
            super(IncidentStatus.class);
        }
    }
}
