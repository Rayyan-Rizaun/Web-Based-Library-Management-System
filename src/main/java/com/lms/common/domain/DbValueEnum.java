package com.lms.common.domain;

/**
 * An enum whose stored database value is not a legal Java constant name.
 *
 * <p>{@code @Enumerated(EnumType.STRING)} persists {@code name()}, so it only
 * works when every value in a column's CHECK constraint is also a valid Java
 * identifier. Five domains in 01_schema.sql contain a space — "Academic Staff",
 * "On Loan", "Written Off", "Under Appeal", "Under Review" and others — and a
 * Java constant cannot. Those five enums implement this interface and are
 * mapped with a {@link DbValueEnumConverter} instead.
 */
public interface DbValueEnum {

    /** The exact text stored in the database column. */
    String dbValue();
}
