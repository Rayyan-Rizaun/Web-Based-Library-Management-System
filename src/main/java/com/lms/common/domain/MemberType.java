package com.lms.common.domain;

/**
 * {@code Member.MemberType} — CK_Member_MemberType.
 *
 * <p>This enum is the whole of the MEMBER → ACADEMIC_STAFF ISA
 * (00_relational_mapping.md §1.2, Strategy C): an academic staff member is
 * a {@link Member} whose type is {@link #AcademicStaff}. There is no
 * AcademicStaff table, entity or inheritance.
 *
 * <p>Some stored values contain a space, which no Java constant name can,
 * so the column is mapped with {@link JpaConverter} rather than
 * {@code @Enumerated} — see {@link DbValueEnum}.
 */
public enum MemberType implements DbValueEnum {

    Student("Student"),
    AcademicStaff("Academic Staff");

    private final String dbValue;

    MemberType(String dbValue) {
        this.dbValue = dbValue;
    }

    @Override
    public String dbValue() {
        return dbValue;
    }

    @jakarta.persistence.Converter
    public static class JpaConverter extends DbValueEnumConverter<MemberType> {
        public JpaConverter() {
            super(MemberType.class);
        }
    }
}
