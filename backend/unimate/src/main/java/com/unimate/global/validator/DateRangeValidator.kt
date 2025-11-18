package com.unimate.global.validator

import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import org.springframework.beans.BeanWrapperImpl
import java.time.LocalDate

class DateRangeValidator : ConstraintValidator<ValidDateRange, Any> {

    private lateinit var startDate: String
    private lateinit var endDate: String

    override fun initialize(constraintAnnotation: ValidDateRange) {
        this.startDate = constraintAnnotation.startDate
        this.endDate = constraintAnnotation.endDate
    }

    override fun isValid(value: Any?, context: ConstraintValidatorContext): Boolean {
        if (value == null) {
            return true
        }

        val startDateValue = BeanWrapperImpl(value).getPropertyValue(startDate)
        val endDateValue = BeanWrapperImpl(value).getPropertyValue(endDate)

        if (startDateValue == null || endDateValue == null) {
            return true // @NotNull annotation should handle this
        }

        return if (startDateValue is LocalDate && endDateValue is LocalDate) {
            !startDateValue.isAfter(endDateValue)
        } else {
            false
        }
    }
}
