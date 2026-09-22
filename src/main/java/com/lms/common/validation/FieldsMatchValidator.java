package com.lms.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;

/**
 * Reads {@link FieldsMatch#first()} and {@link FieldsMatch#second()} off
 * the validated object with Spring's {@link BeanWrapper} (the same
 * property-access machinery Thymeleaf's {@code th:field} uses), compares
 * them with {@code Objects.equals}, and — on a mismatch — reports the
 * error against {@code second} specifically, not the object as a whole, so
 * {@code components/form-field.html}'s {@code #fields.hasErrors('second')}
 * picks it up under the right input.
 */
public class FieldsMatchValidator implements ConstraintValidator<FieldsMatch, Object> {

    private String first;
    private String second;

    @Override
    public void initialize(FieldsMatch constraintAnnotation) {
        this.first = constraintAnnotation.first();
        this.second = constraintAnnotation.second();
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        BeanWrapper wrapper = PropertyAccessorFactory.forBeanPropertyAccess(value);
        Object firstValue = wrapper.getPropertyValue(first);
        Object secondValue = wrapper.getPropertyValue(second);

        if (java.util.Objects.equals(firstValue, secondValue)) {
            return true;
        }

        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                .addPropertyNode(second)
                .addConstraintViolation();
        return false;
    }
}
