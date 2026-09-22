package com.lms.common.domain;

import jakarta.persistence.AttributeConverter;

/**
 * Converts a {@link DbValueEnum} to and from its database text. Each such
 * enum declares a one-line subclass named {@code JpaConverter}, referenced
 * from the entity field with {@code @Convert}.
 */
public abstract class DbValueEnumConverter<E extends Enum<E> & DbValueEnum>
        implements AttributeConverter<E, String> {

    private final Class<E> enumType;

    protected DbValueEnumConverter(Class<E> enumType) {
        this.enumType = enumType;
    }

    @Override
    public String convertToDatabaseColumn(E value) {
        return value == null ? null : value.dbValue();
    }

    @Override
    public E convertToEntityAttribute(String dbValue) {
        if (dbValue == null) {
            return null;
        }
        for (E constant : enumType.getEnumConstants()) {
            if (constant.dbValue().equals(dbValue)) {
                return constant;
            }
        }
        throw new IllegalArgumentException("'" + dbValue + "' is not a " + enumType.getSimpleName()
                + " value; the enum is out of step with its CHECK constraint in 01_schema.sql");
    }
}
