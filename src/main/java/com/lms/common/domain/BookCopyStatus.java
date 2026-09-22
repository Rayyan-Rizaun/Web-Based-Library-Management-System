package com.lms.common.domain;

/**
 * {@code BookCopy.Status} — CK_BookCopy_Status. A kept status column,
 * written only by the D5 triggers (R24).
 *
 * <p>Some stored values contain a space, which no Java constant name can,
 * so the column is mapped with {@link JpaConverter} rather than
 * {@code @Enumerated} — see {@link DbValueEnum}.
 */
public enum BookCopyStatus implements DbValueEnum {

    Available("Available"),
    OnLoan("On Loan"),
    OnHold("On Hold"),
    UnderRepair("Under Repair"),
    Damaged("Damaged"),
    Lost("Lost"),
    Withdrawn("Withdrawn");

    private final String dbValue;

    BookCopyStatus(String dbValue) {
        this.dbValue = dbValue;
    }

    @Override
    public String dbValue() {
        return dbValue;
    }

    @jakarta.persistence.Converter
    public static class JpaConverter extends DbValueEnumConverter<BookCopyStatus> {
        public JpaConverter() {
            super(BookCopyStatus.class);
        }
    }
}
