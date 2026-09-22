package com.lms.common.web;

/**
 * One {@code <option>} for a {@code type='select'} field in
 * {@code components/form-field.html}.
 *
 * <pre>
 *   List.of(
 *       new SelectOption("Student", "Student"),
 *       new SelectOption("Academic Staff", "Academic Staff"));
 * </pre>
 */
public record SelectOption(String value, String label) {
}
