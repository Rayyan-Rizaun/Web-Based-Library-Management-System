package com.lms.common.domain;

/**
 * {@code SystemSetting.SettingDataType} — CK_SystemSetting_DataType.
 * ({@code String} is the constant for the stored text "String", not {@link java.lang.String}.)
 *
 * <p>Constant names are the stored values, so the column is mapped with
 * {@code @Enumerated(EnumType.STRING)}.
 */
public enum SettingDataType {
    Int, Decimal, String, Bool
}
