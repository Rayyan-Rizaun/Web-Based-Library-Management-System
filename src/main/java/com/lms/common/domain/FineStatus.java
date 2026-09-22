package com.lms.common.domain;

/**
 * {@code Fine.Status} — CK_Fine_Status. A kept status column, written only
 * by the D5 triggers (R24).
 *
 * <p>Some stored values contain a space, which no Java constant name can,
 * so the column is mapped with {@link JpaConverter} rather than
 * {@code @Enumerated} — see {@link DbValueEnum}.
 */
public enum FineStatus implements DbValueEnum {

    Pending("Pending"),
    UnderAppeal("Under Appeal"),
    PartiallyPaid("Partially Paid"),
    FullyPaid("Fully Paid"),
    Waived("Waived");

    private final String dbValue;

    FineStatus(String dbValue) {
        this.dbValue = dbValue;
    }

    @Override
    public String dbValue() {
        return dbValue;
    }

    @jakarta.persistence.Converter
    public static class JpaConverter extends DbValueEnumConverter<FineStatus> {
        public JpaConverter() {
            super(FineStatus.class);
        }
    }
}
